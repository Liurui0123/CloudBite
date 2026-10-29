import { defineStore } from 'pinia'

const KEY = 'cb_cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: JSON.parse(localStorage.getItem(KEY) || '[]')
  }),
  getters: {
    count: (s) => s.items.reduce((n, i) => n + i.quantity, 0),
    total: (s) => s.items.reduce((n, i) => n + i.price * i.quantity, 0)
  },
  actions: {
    persist() {
      localStorage.setItem(KEY, JSON.stringify(this.items))
    },
    add(dish) {
      const hit = this.items.find((i) => i.dishId === dish.id)
      if (hit) {
        hit.quantity += 1
      } else {
        this.items.push({
          dishId: dish.id,
          name: dish.name,
          price: Number(dish.price),
          emoji: dish.emoji,
          quantity: 1
        })
      }
      this.persist()
    },
    inc(item) {
      item.quantity += 1
      this.persist()
    },
    dec(item) {
      item.quantity -= 1
      if (item.quantity <= 0) this.remove(item)
      else this.persist()
    },
    remove(item) {
      this.items = this.items.filter((i) => i.dishId !== item.dishId)
      this.persist()
    },
    clear() {
      this.items = []
      this.persist()
    }
  }
})