const FIRST_RETRY_DELAY = 500;
const LONGEST_RETRY_DELAY = 10_000;

export class BoardSocket {
  #url;
  #listener;
  #socket = null;
  #retryDelay = FIRST_RETRY_DELAY;

  constructor(roomId, listener) {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    this.#url = `${scheme}://${location.host}/ws/rooms/${roomId}`;
    this.#listener = listener;
  }

  open() {
    this.#socket = new WebSocket(this.#url);
    this.#socket.addEventListener('open', () => this.#listener.opened());
    this.#socket.addEventListener('message', (event) => this.#listener.received(JSON.parse(event.data)));
    this.#socket.addEventListener('close', (event) => this.#listener.closed(event.code, event.reason));
  }

  send(message) {
    if (this.#socket?.readyState === WebSocket.OPEN) {
      this.#socket.send(JSON.stringify(message));
    }
  }

  reopenLater() {
    setTimeout(() => this.open(), this.#retryDelay);
    this.#retryDelay = Math.min(this.#retryDelay * 2, LONGEST_RETRY_DELAY);
  }

  resetRetryDelay() {
    this.#retryDelay = FIRST_RETRY_DELAY;
  }
}
