export class PeopleList {
  #element;
  #members = [];

  constructor(element) {
    this.#element = element;
  }

  get ids() {
    return this.#members.map((member) => member.id);
  }

  show(members, myId) {
    this.#members = members;
    this.#element.replaceChildren(...members.map((member) => avatarOf(member, member.id === myId)));
  }

  find(memberId) {
    return this.#members.find((member) => member.id === memberId);
  }

  nameOf(memberId) {
    return this.find(memberId)?.name ?? 'Someone';
  }
}

function avatarOf(member, isMe) {
  const avatar = document.createElement('span');
  avatar.className = isMe ? 'avatar self' : 'avatar';
  avatar.style.setProperty('--color', member.color);
  avatar.textContent = initialsOf(member.name);
  avatar.title = isMe ? `${member.name} (you)` : member.name;
  return avatar;
}

function initialsOf(name) {
  return name.split(' ').slice(0, 2).map((word) => [...word][0]).join('').toUpperCase();
}
