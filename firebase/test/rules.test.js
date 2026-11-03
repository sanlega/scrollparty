import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, query, setDoc, updateDoc, where } from 'firebase/firestore';

let env;
const db = (uid) => env.authenticatedContext(uid).firestore();
const member = (name) => ({ name, emoji: '😀', count: 0, joinedAt: 1 });

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-scrollparty',
    firestore: { rules: readFileSync('firestore.rules', 'utf8') },
  });
});
after(() => env.cleanup());
beforeEach(() => env.clearFirestore());

async function seed(status = 'collecting') {
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, 'groups/ABC123'), { adminId: 'ana', status, roundIds: [], roundIndex: 0 });
    await setDoc(doc(f, 'groups/ABC123/members/ana'), member('Ana'));
    await setDoc(doc(f, 'groups/ABC123/members/bob'), member('Bob'));
    await setDoc(doc(f, 'groups/ABC123/members/eva'), member('Eva'));
    await setDoc(doc(f, 'groups/ABC123/links/l1'), { url: 'https://youtu.be/1', authorId: 'bob' });
    await setDoc(doc(f, 'groups/ABC123/rounds/l1'), { url: 'https://youtu.be/1', authorId: null, votes: {} });
  });
}

test('cualquiera autenticado obtiene un grupo por código, pero no puede listarlos', async () => {
  await seed();
  await assertSucceeds(getDoc(doc(db('zoe'), 'groups/ABC123')));
  await assertFails(getDocs(collection(db('zoe'), 'groups')));
});

test('sin sesión no se lee nada', async () => {
  await seed();
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'groups/ABC123')));
});

test('no se puede crear un grupo a nombre de otro', async () => {
  await assertFails(setDoc(doc(db('zoe'), 'groups/NEW111'), { adminId: 'ana', status: 'collecting' }));
  await assertSucceeds(setDoc(doc(db('zoe'), 'groups/NEW111'), { adminId: 'zoe', status: 'collecting' }));
});

test('solo el admin cambia el grupo y no puede regalar el admin', async () => {
  await seed();
  await assertFails(updateDoc(doc(db('bob'), 'groups/ABC123'), { status: 'playing' }));
  await assertSucceeds(updateDoc(doc(db('ana'), 'groups/ABC123'), { status: 'playing' }));
  await assertFails(updateDoc(doc(db('ana'), 'groups/ABC123'), { adminId: 'bob' }));
});

test('unirse crea solo el propio miembro', async () => {
  await seed();
  await assertSucceeds(setDoc(doc(db('zoe'), 'groups/ABC123/members/zoe'), member('Zoe')));
  await assertFails(setDoc(doc(db('zoe'), 'groups/ABC123/members/mal'), member('Mal')));
  await assertFails(getDocs(collection(db('nadie'), 'groups/ABC123/members')));
});

test('antes de la sesión solo el autor ve sus enlaces, ni siquiera el anfitrión', async () => {
  await seed();
  const links = (f) => collection(f, 'groups/ABC123/links');
  await assertSucceeds(getDocs(query(links(db('bob')), where('authorId', '==', 'bob'))));
  await assertFails(getDoc(doc(db('eva'), 'groups/ABC123/links/l1')));
  await assertFails(getDocs(links(db('ana'))));
});

test('en la sesión el anfitrión lee los enlaces y los demás siguen sin poder', async () => {
  await seed('playing');
  await assertSucceeds(getDocs(collection(db('ana'), 'groups/ABC123/links')));
  await assertFails(getDocs(collection(db('eva'), 'groups/ABC123/links')));
});

test('los enlaces deben ser https, propios y solo mientras se recoge', async () => {
  await seed();
  const add = (uid, id, data) => setDoc(doc(db(uid), `groups/ABC123/links/${id}`), data);
  await assertSucceeds(add('eva', 'a', { url: 'https://www.tiktok.com/@a/video/2', authorId: 'eva' }));
  await assertFails(add('eva', 'b', { url: 'javascript:alert(1)', authorId: 'eva' }));
  await assertFails(add('eva', 'b2', { url: 'https://evil.example/x', authorId: 'eva' }));
  await assertFails(add('eva', 'b3', { url: 'https://youtube.com.evil.example/x', authorId: 'eva' }));
  await assertFails(add('eva', 'c', { url: 'https://vm.tiktok.com/3', authorId: 'bob' }));
  await assertFails(add('zoe', 'd', { url: 'https://www.instagram.com/reel/4', authorId: 'zoe' }));
  await seed('playing');
  await assertFails(add('eva', 'e', { url: 'https://youtube.com/shorts/5', authorId: 'eva' }));
  await assertFails(deleteDoc(doc(db('bob'), 'groups/ABC123/links/l1')));
});

test('cada jugador vota solo por sí mismo y no toca nada más', async () => {
  await seed('playing');
  const round = (uid) => doc(db(uid), 'groups/ABC123/rounds/l1');
  await assertSucceeds(updateDoc(round('eva'), { 'votes.eva': 'bob' }));
  await assertFails(updateDoc(round('eva'), { 'votes.ana': 'bob' }));
  await assertFails(updateDoc(round('eva'), { authorId: 'bob' }));
  await assertFails(updateDoc(round('zoe'), { 'votes.zoe': 'bob' }));
});

test('tras revelar no se puede votar, y el anfitrión no puede alterar votos', async () => {
  await seed('playing');
  const round = (uid) => doc(db(uid), 'groups/ABC123/rounds/l1');
  await assertFails(updateDoc(round('ana'), { 'votes.eva': 'ana', authorId: 'bob' }));
  await assertSucceeds(updateDoc(round('ana'), { authorId: 'bob' }));
  await assertFails(updateDoc(round('eva'), { 'votes.eva': 'bob' }));
});
