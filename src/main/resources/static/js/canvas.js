export class StrokeCanvas {
  #canvas;
  #context;
  #strokes = new Map();
  #topStrokeId = null;
  #redrawQueued = false;

  constructor(canvas) {
    this.#canvas = canvas;
    this.#context = canvas.getContext('2d');
    this.resize();
  }

  get isEmpty() {
    return this.#strokes.size === 0;
  }

  replaceAll(strokes) {
    this.#strokes.clear();
    strokes.forEach(({ id, color, width, points }) => {
      this.#strokes.set(id, { color, width, points: [...points] });
    });
    this.#topStrokeId = strokes.at(-1)?.id ?? null;
    this.#redrawSoon();
  }

  extend({ id, color, width, points }) {
    const stroke = this.#strokes.get(id) ?? this.#add(id, color, width);
    const lastDrawnPoint = Math.max(stroke.points.length - 2, 0);
    stroke.points.push(...points);
    if (id === this.#topStrokeId) {
      this.#draw(stroke, lastDrawnPoint);
    } else {
      this.#redrawSoon();
    }
  }

  remove(id) {
    const removed = this.#strokes.delete(id);
    if (removed) {
      this.#topStrokeId = [...this.#strokes.keys()].at(-1) ?? null;
      this.#redrawSoon();
    }
    return removed;
  }

  clear() {
    this.#strokes.clear();
    this.#topStrokeId = null;
    this.#redrawSoon();
  }

  resize() {
    const ratio = window.devicePixelRatio || 1;
    this.#canvas.width = Math.round(innerWidth * ratio);
    this.#canvas.height = Math.round(innerHeight * ratio);
    this.#context.setTransform(ratio, 0, 0, ratio, 0, 0);
    this.#context.lineCap = 'round';
    this.#context.lineJoin = 'round';
    this.#redrawSoon();
  }

  #add(id, color, width) {
    const stroke = { color, width, points: [] };
    this.#strokes.set(id, stroke);
    this.#topStrokeId = id;
    return stroke;
  }

  #redrawSoon() {
    if (this.#redrawQueued) {
      return;
    }
    this.#redrawQueued = true;
    requestAnimationFrame(() => {
      this.#redrawQueued = false;
      this.#context.clearRect(0, 0, innerWidth, innerHeight);
      this.#strokes.forEach((stroke) => this.#draw(stroke, 0));
    });
  }

  #draw({ color, width, points }, from) {
    const context = this.#context;
    context.strokeStyle = color;
    context.lineWidth = width;
    context.beginPath();
    context.moveTo(points[from], points[from + 1]);
    for (let i = from + 2; i < points.length; i += 2) {
      context.lineTo(points[i], points[i + 1]);
    }
    if (points.length - from === 2) {
      context.lineTo(points[from], points[from + 1]);
    }
    context.stroke();
  }
}
