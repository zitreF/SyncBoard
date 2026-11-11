const POINTER = '<svg viewBox="0 0 18 22"><path d="M2 2v16l4.5-4.2 3.1 6.7 2.8-1.3-3.1-6.6 5.7-.4z"/></svg>';

export class CursorLayer {
  #layer;
  #cursors = new Map();

  constructor(layer) {
    this.#layer = layer;
  }

  move(member, x, y) {
    const cursor = this.#cursors.get(member.id) ?? this.#add(member);
    cursor.style.transform = `translate(${x}px, ${y}px)`;
  }

  remove(memberId) {
    this.#cursors.get(memberId)?.remove();
    this.#cursors.delete(memberId);
  }

  keepOnly(memberIds) {
    [...this.#cursors.keys()]
      .filter((memberId) => !memberIds.includes(memberId))
      .forEach((memberId) => this.remove(memberId));
  }

  clear() {
    this.keepOnly([]);
  }

  #add(member) {
    const cursor = document.createElement('div');
    cursor.className = 'cursor';
    cursor.style.setProperty('--color', member.color);
    cursor.innerHTML = `${POINTER}<span class="cursor-label"></span>`;
    cursor.querySelector('.cursor-label').textContent = member.name;
    this.#layer.append(cursor);
    this.#cursors.set(member.id, cursor);
    return cursor;
  }
}
