const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const TEACHER_UID = "teacher_101";
const STUDENT_UID = "student_202";
const OTHER_UID = "student_303";
const ROOM_CODE = "SB4829";

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

function validClassroomPayload(overrides = {}) {
  const now = new Date();
  return {
    roomCode: ROOM_CODE,
    title: "Physics Live Board",
    teacherUid: TEACHER_UID,
    teacherName: "Prof. Rahman",
    status: "active",
    allowStudentDrawing: false,
    currentPageIndex: 0,
    participantCount: 1,
    pagesJson: "[]",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or create classrooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("classrooms").doc(ROOM_CODE).get());
  await assertFails(unauthDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload()));
});

test("Teacher can create classroom and student can read active classroom", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  await assertSucceeds(
    teacherDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload())
  );

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  await assertSucceeds(studentDb.collection("classrooms").doc(ROOM_CODE).get());
});

test("Identity spoofing blocked: student cannot create classroom claiming teacherUid", async () => {
  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  await assertFails(
    studentDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload({ teacherUid: TEACHER_UID }))
  );
});

test("View-only enforcement: student cannot update pagesJson when allowStudentDrawing is false", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  await assertSucceeds(
    teacherDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload({ allowStudentDrawing: false }))
  );

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  await assertFails(
    studentDb.collection("classrooms").doc(ROOM_CODE).update({
      pagesJson: "[{\"id\":\"p1\"}]",
      updatedAt: new Date(),
    })
  );
});

test("Authorized student can update pagesJson when allowStudentDrawing is true", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  await assertSucceeds(
    teacherDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload({ allowStudentDrawing: true }))
  );

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  await assertSucceeds(
    studentDb.collection("classrooms").doc(ROOM_CODE).update({
      pagesJson: "[{\"id\":\"p1\"}]",
      updatedAt: new Date(),
    })
  );
});

test("Student can join members subcollection and Teacher can toggle student canDraw permission", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  await assertSucceeds(
    teacherDb.collection("classrooms").doc(ROOM_CODE).set(validClassroomPayload())
  );

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    studentDb.collection("classrooms").doc(ROOM_CODE).collection("members").doc(STUDENT_UID).set({
      userId: STUDENT_UID,
      roomCode: ROOM_CODE,
      displayName: "Arif",
      role: "student",
      canDraw: false,
      joinedAt: now,
      updatedAt: now,
    })
  );

  // Student cannot self-promote canDraw to true
  await assertFails(
    studentDb.collection("classrooms").doc(ROOM_CODE).collection("members").doc(STUDENT_UID).update({
      canDraw: true,
      updatedAt: new Date(),
    })
  );

  // Teacher CAN grant canDraw to student
  await assertSucceeds(
    teacherDb.collection("classrooms").doc(ROOM_CODE).collection("members").doc(STUDENT_UID).update({
      canDraw: true,
      updatedAt: new Date(),
    })
  );
});
