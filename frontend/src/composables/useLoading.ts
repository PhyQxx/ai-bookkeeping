import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useLoading() {
  const loading = ref(false)

  async function withLoading<T>(fn: () => Promise<T>): Promise<T | undefined> {
    loading.value = true
    try {
      return await fn()
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
      return undefined
    } finally {
      loading.value = false
    }
  }

  return { loading, withLoading }
}
