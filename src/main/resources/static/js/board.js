import { StrokeCanvas } from './canvas.js';
import { CursorLayer } from './cursors.js';
import { JoinDialog } from './join-dialog.js';
import { showGone, showStatus, showToast } from './notices.js';
import { Pen } from './pen.js';
import { PeopleList } from './people.js';
import { BoardSocket } from './socket.js';
import { throttled } from './throttle.js';

const CURSOR_INTERVAL = 40;
const WRONG_CODE = 4403;
const FIRST_REFUSAL_CODE = 4000;

const roomId = location.pathname.split('/').pop();
const canvasElement = document.getElementById('canvas');
const shareButton = document.getElementById('share');

const canvas = new StrokeCanvas(canvasElement);
const cursors = new CursorLayer(document.getElementById('cursors'));
const people = new PeopleList(document.getElementById('people'));
const joinDialog = new JoinDialog(document.getElementById('join'), roomId, connect);
const socket = new BoardSocket(roomId, { opened: join, received: receive, closed: disconnected });
const pen = new Pen(canvas, (message) => socket.send(message));

let myId = null;
let joined = false;
let latestCursor = null;

const sendCursorSoon = throttled(() => socket.send(latestCursor), CURSOR_INTERVAL);

async function start() {
  const room = await findRoom();
  if (room === null) {
    return;
  }
  document.getElementById('room-id').textContent = room.id;
  document.getElementById('locked').hidden = !room.locked;
  joinDialog.prepare(room);
  if (joinDialog.isFilledIn) {
    connect();
  } else {
    askToJoin();
  }
}

async function findRoom() {
  try {
    const response = await fetch(`/api/rooms/${roomId}`);
    if (response.ok) {
      return await response.json();
    }
    showGone(response.status === 404 ? 'This board does not exist or has expired.' : 'Something went wrong.');
  } catch {
    showGone('The server is not reachable.');
  }
  return null;
}

function connect() {
  showStatus('connecting', 'Connecting');
  socket.open();
}

function join() {
  socket.send({ type: 'join', name: joinDialog.name, code: joinDialog.code });
}

function askToJoin() {
  showStatus('offline', 'Not joined');
  joinDialog.ask();
}

function disconnected(code, reason) {
  joined = false;
  pen.cancel();
  cursors.clear();
  if (code === WRONG_CODE) {
    showStatus('offline', 'Not joined');
    joinDialog.askForCode('That code is not right.');
  } else if (code >= FIRST_REFUSAL_CODE) {
    showGone(reason || 'You were disconnected from this board.');
  } else {
    showStatus('offline', 'Reconnecting');
    socket.reopenLater();
  }
}

function receive(message) {
  switch (message.type) {
    case 'welcome':
      welcomed(message);
      break;
    case 'members':
      showMembers(message.members);
      break;
    case 'draw':
      canvas.extend(message);
      break;
    case 'erase':
      canvas.remove(message.id);
      pen.cancelStroke(message.id);
      break;
    case 'clear':
      canvas.clear();
      pen.restart();
      showToast(`${people.nameOf(message.by)} cleared the board`);
      break;
    case 'cursor':
      showCursor(message);
      break;
    case 'notice':
      showToast(message.message);
      break;
  }
}

function welcomed(message) {
  joined = true;
  myId = message.you;
  socket.resetRetryDelay();
  canvas.replaceAll(message.strokes);
  showMembers(message.members);
  showStatus('live', 'Live');
}

function showMembers(members) {
  people.show(members, myId);
  cursors.keepOnly(people.ids);
}

function showCursor({ member: memberId, x, y }) {
  const member = people.find(memberId);
  if (member && x !== null && y !== null) {
    cursors.move(member, x, y);
  } else {
    cursors.remove(memberId);
  }
}

function sendCursor(x, y) {
  if (joined) {
    latestCursor = { type: 'cursor', x, y };
    sendCursorSoon();
  }
}

function undo() {
  if (joined) {
    pen.undo();
  }
}

function clearBoard() {
  if (joined && !canvas.isEmpty && confirm('Clear the board for everyone?')) {
    canvas.clear();
    pen.restart();
    socket.send({ type: 'clear' });
  }
}

async function copyLink() {
  const link = `${location.origin}/r/${roomId}`;
  try {
    await navigator.clipboard.writeText(link);
    shareButton.textContent = 'Copied';
    setTimeout(() => {
      shareButton.textContent = 'Copy link';
    }, 1500);
  } catch {
    prompt('Copy this link:', link);
  }
}

function press(selector, chosen) {
  document.querySelectorAll(selector).forEach((button) => {
    button.setAttribute('aria-pressed', String(button === chosen));
  });
}

function pointsOf(event) {
  const events = event.getCoalescedEvents?.() ?? [];
  return events.length > 0 ? events : [event];
}

canvasElement.addEventListener('pointerdown', (event) => {
  if (!joined || pen.isDrawing || event.button !== 0) {
    return;
  }
  canvasElement.setPointerCapture(event.pointerId);
  pen.start(Math.round(event.clientX), Math.round(event.clientY));
});

canvasElement.addEventListener('pointermove', (event) => {
  sendCursor(Math.round(event.clientX), Math.round(event.clientY));
  pointsOf(event).forEach((point) => pen.moveTo(Math.round(point.clientX), Math.round(point.clientY)));
});

canvasElement.addEventListener('pointerup', () => pen.finish());
canvasElement.addEventListener('pointercancel', () => pen.finish());
canvasElement.addEventListener('pointerleave', () => sendCursor(null, null));

document.querySelectorAll('[data-color]').forEach((button) => {
  button.style.setProperty('--swatch', button.dataset.color);
  button.addEventListener('click', () => {
    pen.color = button.dataset.color;
    press('[data-color]', button);
  });
});

document.querySelectorAll('[data-width]').forEach((button) => {
  button.addEventListener('click', () => {
    pen.width = Number(button.dataset.width);
    press('[data-width]', button);
  });
});

document.getElementById('undo').addEventListener('click', undo);
document.getElementById('clear').addEventListener('click', clearBoard);
shareButton.addEventListener('click', copyLink);

window.addEventListener('keydown', (event) => {
  const isUndo = (event.ctrlKey || event.metaKey) && !event.shiftKey && event.key.toLowerCase() === 'z';
  if (isUndo && !joinDialog.isOpen) {
    event.preventDefault();
    undo();
  }
});

window.addEventListener('resize', () => canvas.resize());

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    sendCursor(null, null);
  }
});

start();
