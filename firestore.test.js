const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProjectPayload(uid, overrides = {}) {
  const ts = new Date(Date.now() - 2000);
  return {
    userId: uid,
    title: "Neon Summer Poster",
    category: "POSTER",
    aspectRatio: "3:4",
    backgroundType: "GRADIENT",
    backgroundColorHex: "#14121F",
    secondaryColorHex: "#FF5A36",
    filterPreset: "ORIGINAL",
    brightness: 0.0,
    contrast: 1.0,
    saturation: 1.0,
    overlayOpacity: 0.2,
    layers: [
      {
        id: "layer_1",
        type: "TEXT",
        text: "SUMMER FEST",
        fontId: "SPACE_GROTESK",
        colorHex: "#FFFFFF",
        fontSizeSp: 32,
        xFraction: 0.5,
        yFraction: 0.35,
        uppercase: true,
        badgeBgHex: "",
      },
    ],
    favorite: false,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write user profile, projects, or ai_generations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProjectPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ai_generations").get());
});

test("User profile create, read, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      preferredFormat: "POSTER_3_4",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredFormat: "BANNER_16_9",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field & cross-user access
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("DesignProject CRUD, partial updates, and validation parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create (matches Kotlin toCreateMap without @DocumentId id)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProjectPayload(ALICE_UID))
  );

  // List own projects (unfiltered subcollection query per Kotlin repository)
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());

  // Toggle favorite partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      favorite: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full canvas save update (preserves createdAt)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      title: "Updated Poster",
      brightness: 0.25,
      contrast: 1.2,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject extra @DocumentId 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_2").set(
      validProjectPayload(ALICE_UID, { id: "proj_2" })
    )
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_3").set(
      validProjectPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  // Reject out-of-range brightness
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_4").set(
      validProjectPayload(ALICE_UID, { brightness: 5.0 })
    )
  );

  // Reject Bob reading/listing/updating/deleting Alice's projects
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      title: "Stolen",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").delete());
});

test("AiGenerationRecord create, list, immutable update block, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_generations").doc("gen_1").set({
      userId: ALICE_UID,
      mode: "POSTER_BANNER",
      prompt: "Minimalist jazz festival poster with warm brass highlights",
      stylePreset: "Editorial Minimal",
      aspectRatio: "3:4",
      resultSummary: "Generated 3:4 poster artwork",
      createdAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("ai_generations").get());

  // Updates are disallowed on immutable history records
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_generations").doc("gen_1").update({
      prompt: "Tampered",
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ai_generations").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("ai_generations").doc("gen_1").delete());
});
