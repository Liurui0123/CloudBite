export const STATUS_MAP = {
  PENDING_PAYMENT: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'primary' },
  COOKING: { text: '制作中', type: 'info' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' }
}

export function statusText(s) {
  return (STATUS_MAP[s] || { text: s }).text
}

export function statusType(s) {
  return (STATUS_MAP[s] || { type: 'info' }).type
}