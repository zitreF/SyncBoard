import { throttled } from './throttle.js';

const SEND_INTERVAL = 40;
const MAX_STROKE_COORDINATES = 2000;

export class Pen {
  color = '#1e1e1e';
  width = 4;

  #canvas;
  #send;
  #sendSoon;
  #stroke = null;
  #unsent = [];
  #ownStrokeIds = [];

  constructor(canvas, send) {
    this.#canvas = canvas;
    this.#send = send;
    this.#sendSoon = throttled(() => this.#sendUnsent(), SEND_INTERVAL);
  }

  get isDrawing() {
    return this.#stroke !== null;
  }

  start(x, y) {
    this.#beginStroke();
    this.#addPoint(x, y);
  }

  moveTo(x, y) {
    if (!this.isDrawing || this.#isAt(x, y)) {
      return;
    }
    if (this.#stroke.length >= MAX_STROKE_COORDINATES) {
      this.#sendUnsent();
      this.#continueInNewStroke();
    }
    this.#addPoint(x, y);
  }

  finish() {
    this.#sendUnsent();
    this.#stroke = null;
  }

  cancel() {
    this.#stroke = null;
    this.#unsent = [];
  }

  cancelStroke(id) {
    if (this.#stroke?.id === id) {
      this.cancel();
    }
  }

  restart() {
    if (this.isDrawing) {
      this.#continueInNewStroke();
    }
  }

  undo() {
    if (this.isDrawing) {
      return;
    }
    while (this.#ownStrokeIds.length > 0) {
      const id = this.#ownStrokeIds.pop();
      if (this.#canvas.remove(id)) {
        this.#send({ type: 'erase', id });
        return;
      }
    }
  }

  #beginStroke() {
    this.#stroke = { id: randomId(), color: this.color, width: this.width, x: null, y: null, length: 0 };
    this.#unsent = [];
    this.#ownStrokeIds.push(this.#stroke.id);
  }

  #continueInNewStroke() {
    const { x, y } = this.#stroke;
    this.#beginStroke();
    this.#addPoint(x, y);
  }

  #addPoint(x, y) {
    const { id, color, width } = this.#stroke;
    this.#stroke.x = x;
    this.#stroke.y = y;
    this.#stroke.length += 2;
    this.#unsent.push(x, y);
    this.#canvas.extend({ id, color, width, points: [x, y] });
    this.#sendSoon();
  }

  #isAt(x, y) {
    return this.#stroke.x === x && this.#stroke.y === y;
  }

  #sendUnsent() {
    if (!this.isDrawing || this.#unsent.length === 0) {
      return;
    }
    const { id, color, width } = this.#stroke;
    this.#send({ type: 'draw', id, color, width, points: this.#unsent });
    this.#unsent = [];
  }
}

function randomId() {
  return Array.from(crypto.getRandomValues(new Uint32Array(3)), (number) => number.toString(36)).join('');
}
