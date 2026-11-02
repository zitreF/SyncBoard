import { load, save } from './storage.js';

const SEND_INTERVAL = 40;
const MAX_STROKE_COORDINATES = 2000;

const roomId = location.pathname.split('/').pop();
const canvas = document.getElementById('canvas');
const context = canvas.getContext('2d');
const cursorLayer = document.getElementById('cursors');
const people = document.getElementById('people');
const status = document.getElementById('status');
const toast = document.getElementById('toast');
const dialog = document.getElementById('join');
const nameInput = document.getElementById('name');
const codeInput = document.getElementById('code');
const codeField = document.getElementById('code-field');
const joinError = document.getElementById('join-error');
const shareButton = document.getElementById('share');

const strokes = new Map();
const cursors = new Map();
const myStrokes = [];
let members = [];
let me = null;
let socket = null;
let joined = false;
let retryDelay = 500;
let color = '#1e1e1e';
let width = 4;
let current = null;
let unsent = [];
let nextCursor = null;
let cursorTimer = null;
let toastTimer = null;
let ratio = 1;
let redrawQueued = false;

async function start() {
  resize();
  let room;
  try {
    const response = await fetch(`/api/rooms/${roomId}`);
    if (!response.ok) {
      showGone(response.status === 404 ? 'This board does not exist or has expired.' : 'Something went wrong.');
      return;
    }
    room = await response.json();
  } catch {
    showGone('The server is not reachable.');
    return;
  }
  document.getElementById('room-id').textContent = room.id;
  document.getElementById('locked').hidden = !room.locked;
  codeField.hidden = !room.locked;
  codeInput.required = room.locked;
  nameInput.value = load('local', 'syncboard.name') ?? '';
  codeInput.value = load('session', `syncboard.code.${roomId}`) ?? '';
  if (nameInput.value && (!room.locked || codeInput.value)) {
    connect();
  } else {
    setStatus('offline', 'Not joined');
    dialog.showModal();
  }
}

function connect() {
  setStatus('connecting', 'Connecting');
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
  socket = new WebSocket(`${scheme}://${location.host}/ws/rooms/${roomId}`);
  socket.addEventListener('open', () => {
    send({ type: 'join', name: nameInput.value.trim(), code: codeInput.value.trim() });
  });
  socket.addEventListener('message', (event) => receive(JSON.parse(event.data)));
  socket.addEventListener('close', disconnected);
}

function disconnected(event) {
  joined = false;
  socket = null;
  current = null;
  unsent = [];
  [...cursors.keys()].forEach(removeCursor);
  if (event.code === 4403) {
    setStatus('offline', 'Not joined');
    joinError.textContent = 'That code is not right.';
    codeField.hidden = false;
    dialog.showModal();
  } else if (event.code >= 4000) {
    showGone(event.reason || 'You were disconnected from this board.');
  } else {
    setStatus('offline', 'Reconnecting');
    setTimeout(connect, retryDelay);
    retryDelay = Math.min(retryDelay * 2, 10_000);
  }
}

function receive(message) {
  switch (message.type) {
    case 'welcome':
      joined = true;
      retryDelay = 500;
      strokes.clear();
      message.strokes.forEach((stroke) => strokes.set(stroke.id, stroke));
      me = message.you;
      showMembers(message.members);
      setStatus('live', 'Live');
      redraw();
      break;
    case 'members':
      showMembers(message.members);
      break;
    case 'draw':
      if (strokes.has(message.id)) {
        strokes.get(message.id).points.push(...message.points);
      } else {
        strokes.set(message.id, { id: message.id, color: message.color, width: message.width, points: message.points });
      }
      redraw();
      break;
    case 'erase':
      strokes.delete(message.id);
      if (current?.id === message.id) {
        current = null;
        unsent = [];
      }
      redraw();
      break;
    case 'clear':
      strokes.clear();
      if (current) {
        strokes.set(current.id, current);
      }
      showToast(`${nameOf(message.by)} cleared the board`);
      redraw();
      break;
    case 'cursor':
      moveCursor(message.member, message.x, message.y);
      break;
    case 'notice':
      showToast(message.message);
      break;
  }
}

function send(message) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(message));
  }
}

function startStroke() {
  current = { id: randomId(), color, width, points: [] };
  strokes.set(current.id, current);
  myStrokes.push(current.id);
}

function addPoint(clientX, clientY) {
  const x = Math.round(clientX);
  const y = Math.round(clientY);
  const lastX = current.points.at(-2);
  const lastY = current.points.at(-1);
  if (x === lastX && y === lastY) {
    return;
  }
  if (current.points.length >= MAX_STROKE_COORDINATES) {
    sendUnsent();
    startStroke();
    append(lastX, lastY);
  }
  append(x, y);
}

function append(x, y) {
  current.points.push(x, y);
  unsent.push(x, y);
}

function sendUnsent() {
  if (current && unsent.length > 0) {
    send({ type: 'draw', id: current.id, color: current.color, width: current.width, points: unsent });
    unsent = [];
  }
}

function endStroke() {
  if (current) {
    sendUnsent();
    current = null;
  }
}

function undo() {
  if (!joined || current) {
    return;
  }
  while (myStrokes.length > 0) {
    const id = myStrokes.pop();
    if (strokes.delete(id)) {
      send({ type: 'erase', id });
      redraw();
      return;
    }
  }
}

function clearBoard() {
  if (joined && strokes.size > 0 && confirm('Clear the board for everyone?')) {
    strokes.clear();
    send({ type: 'clear' });
    redraw();
  }
}

function queueCursor(x, y) {
  if (!joined) {
    return;
  }
  nextCursor = { type: 'cursor', x, y };
  if (cursorTimer === null) {
    cursorTimer = setTimeout(() => {
      cursorTimer = null;
      send(nextCursor);
    }, SEND_INTERVAL);
  }
}

function moveCursor(memberId, x, y) {
  const member = members.find((candidate) => candidate.id === memberId);
  if (x === null || y === null || !member) {
    removeCursor(memberId);
    return;
  }
  if (!cursors.has(memberId)) {
    const view = document.createElement('div');
    view.className = 'cursor';
    view.style.setProperty('--color', member.color);
    view.innerHTML = '<svg viewBox="0 0 18 22"><path d="M2 2v16l4.5-4.2 3.1 6.7 2.8-1.3-3.1-6.6 5.7-.4z"/></svg>'
      + '<span class="cursor-label"></span>';
    view.querySelector('.cursor-label').textContent = member.name;
    cursorLayer.append(view);
    cursors.set(memberId, view);
  }
  cursors.get(memberId).style.transform = `translate(${x}px, ${y}px)`;
}

function removeCursor(memberId) {
  cursors.get(memberId)?.remove();
  cursors.delete(memberId);
}

function showMembers(list) {
  members = list;
  people.replaceChildren(...list.map((member) => {
    const avatar = document.createElement('span');
    avatar.className = member.id === me ? 'avatar self' : 'avatar';
    avatar.style.setProperty('--color', member.color);
    avatar.textContent = initials(member.name);
    avatar.title = member.id === me ? `${member.name} (you)` : member.name;
    return avatar;
  }));
  [...cursors.keys()]
    .filter((id) => !list.some((member) => member.id === id))
    .forEach(removeCursor);
}

function resize() {
  ratio = window.devicePixelRatio || 1;
  canvas.width = Math.round(innerWidth * ratio);
  canvas.height = Math.round(innerHeight * ratio);
  redraw();
}

function redraw() {
  if (redrawQueued) {
    return;
  }
  redrawQueued = true;
  requestAnimationFrame(() => {
    redrawQueued = false;
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    context.clearRect(0, 0, innerWidth, innerHeight);
    context.lineCap = 'round';
    context.lineJoin = 'round';
    strokes.forEach(drawStroke);
  });
}

function drawStroke({ color, width, points }) {
  context.strokeStyle = color;
  context.lineWidth = width;
  context.beginPath();
  context.moveTo(points[0], points[1]);
  for (let i = 2; i < points.length; i += 2) {
    context.lineTo(points[i], points[i + 1]);
  }
  if (points.length === 2) {
    context.lineTo(points[0], points[1]);
  }
  context.stroke();
}

function setStatus(state, text) {
  status.dataset.state = state;
  status.textContent = text;
}

function showToast(text) {
  toast.textContent = text;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.hidden = true;
  }, 3000);
}

function showGone(text) {
  setStatus('offline', 'Not connected');
  document.getElementById('gone-text').textContent = text;
  document.getElementById('gone').hidden = false;
}

function nameOf(memberId) {
  return members.find((member) => member.id === memberId)?.name ?? 'Someone';
}

function initials(name) {
  return name.split(' ').slice(0, 2).map((word) => [...word][0]).join('').toUpperCase();
}

function randomId() {
  return Array.from(crypto.getRandomValues(new Uint32Array(3)), (n) => n.toString(36)).join('');
}

function press(selector, chosen) {
  document.querySelectorAll(selector).forEach((button) => button.setAttribute('aria-pressed', String(button === chosen)));
}

canvas.addEventListener('pointerdown', (event) => {
  if (!joined || current || event.button !== 0) {
    return;
  }
  canvas.setPointerCapture(event.pointerId);
  startStroke();
  addPoint(event.clientX, event.clientY);
  redraw();
});

canvas.addEventListener('pointermove', (event) => {
  queueCursor(Math.round(event.clientX), Math.round(event.clientY));
  if (current) {
    const coalesced = event.getCoalescedEvents?.() ?? [];
    (coalesced.length > 0 ? coalesced : [event]).forEach((point) => addPoint(point.clientX, point.clientY));
    redraw();
  }
});

canvas.addEventListener('pointerup', endStroke);
canvas.addEventListener('pointercancel', endStroke);
canvas.addEventListener('pointerleave', () => queueCursor(null, null));

document.querySelectorAll('[data-color]').forEach((button) => {
  button.style.setProperty('--swatch', button.dataset.color);
  button.addEventListener('click', () => {
    color = button.dataset.color;
    press('[data-color]', button);
  });
});

document.querySelectorAll('[data-width]').forEach((button) => {
  button.addEventListener('click', () => {
    width = Number(button.dataset.width);
    press('[data-width]', button);
  });
});

document.getElementById('undo').addEventListener('click', undo);
document.getElementById('clear').addEventListener('click', clearBoard);

shareButton.addEventListener('click', async () => {
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
});

dialog.addEventListener('cancel', (event) => event.preventDefault());

document.getElementById('join-form').addEventListener('submit', (event) => {
  event.preventDefault();
  save('local', 'syncboard.name', nameInput.value.trim());
  save('session', `syncboard.code.${roomId}`, codeInput.value.trim());
  joinError.textContent = '';
  dialog.close();
  connect();
});

window.addEventListener('keydown', (event) => {
  if ((event.ctrlKey || event.metaKey) && !event.shiftKey && event.key.toLowerCase() === 'z' && !dialog.open) {
    event.preventDefault();
    undo();
  }
});

window.addEventListener('resize', resize);

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    queueCursor(null, null);
  }
});

start();
