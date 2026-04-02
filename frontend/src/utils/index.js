export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '0.00'
  return Number(amount).toFixed(2)
}

export function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr
}

export function getToday() {
  return new Date().toISOString().split('T')[0]
}

export function getCurrentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
