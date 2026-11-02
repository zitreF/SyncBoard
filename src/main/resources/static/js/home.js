import { save } from './storage.js';

const form = document.getElementById('create');
const codeInput = document.getElementById('code');
const error = document.getElementById('error');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  error.textContent = '';
  const code = codeInput.value.trim();
  try {
    const response = await fetch('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: code || null }),
    });
    if (!response.ok) {
      error.textContent = response.status === 400
        ? 'The join code needs 4 to 32 characters.'
        : 'Could not create a board right now, try again in a moment.';
      return;
    }
    const room = await response.json();
    if (code) {
      save('session', `syncboard.code.${room.id}`, code);
    }
    location.href = `/r/${room.id}`;
  } catch {
    error.textContent = 'The server is not reachable.';
  }
});
