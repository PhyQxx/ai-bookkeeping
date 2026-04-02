/** 通用响应 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

/** 分页 */
export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
}

/** 登录响应 */
export interface LoginVO {
  token: string
  username: string
  nickname: string
  avatar: string
}

/** 账单 */
export interface Bill {
  id: number
  amount: number
  type: number
  typeName: string
  categoryId: number
  categoryName: string
  billDate: string
  remark: string
  inputType: number
  createdAt: string
}

/** 账单请求 */
export interface BillRequest {
  amount: number
  type: number
  categoryId?: number
  billDate?: string
  remark?: string
}

/** AI 解析请求 */
export interface AiParseRequest {
  input: string
}

/** AI 解析响应 */
export interface AiParseVO {
  success: boolean
  amount: number
  category: string
  categoryId: number
  date: string
  remark: string
  errorMessage?: string
  billId?: number
}

/** 分类 */
export interface Category {
  id: number
  name: string
  type: number
  icon: string
  sortOrder: number
  isSystem: boolean
}

/** 分类请求 */
export interface CategoryRequest {
  name: string
  type: number
  icon?: string
  sortOrder?: number
}

/** 月度统计 */
export interface MonthlyStat {
  month: string
  totalIncome: number
  totalExpense: number
  balance: number
  billCount: number
}

/** 分类占比 */
export interface CategoryRatio {
  categoryId: number
  categoryName: string
  amount: number
  ratio: number
}

/** 趋势数据 */
export interface TrendItem {
  month: string
  income: number
  expense: number
}
