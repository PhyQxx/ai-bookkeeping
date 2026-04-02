import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBillList, aiParse as aiParseApi, createBill, updateBill, deleteBill } from '../api/bill'

export const useBillStore = defineStore('bill', () => {
  const billList = ref([])
  const total = ref(0)
  const loading = ref(false)
  const aiResult = ref(null)
  const aiLoading = ref(false)

  async function fetchBills(params = {}) {
    loading.value = true
    try {
      const data = await getBillList({ page: 1, size: 20, ...params })
      billList.value = data.records || data.list || data || []
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function parseAi(input) {
    aiLoading.value = true
    aiResult.value = null
    try {
      aiResult.value = await aiParseApi({ input })
      return aiResult.value
    } finally {
      aiLoading.value = false
    }
  }

  async function addBill(data) {
    return await createBill(data)
  }

  async function editBill(id, data) {
    return await updateBill(id, data)
  }

  async function removeBill(id) {
    return await deleteBill(id)
  }

  return { billList, total, loading, aiResult, aiLoading, fetchBills, parseAi, addBill, editBill, removeBill }
})
