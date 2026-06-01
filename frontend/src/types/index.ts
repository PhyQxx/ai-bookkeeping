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
  currentLedgerId?: number
}

/** 账本 */
export interface Ledger {
  id: number
  name: string
  description?: string
  isDefault: number
  createdAt: string
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

/** AI 解析预览响应 */
export interface AiParsePreviewVO {
  success: boolean
  amount?: number
  category?: string
  categoryId?: number
  date?: string
  remark?: string
  errorMessage?: string
  candidateCategories?: { id: number; name: string }[]
}

/** AI 确认记账请求 */
export interface AiConfirmRequest {
  amount: number
  type: number
  categoryId: number
  date: string
  remark?: string
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

/** 年度统计 */
export interface YearlyStat {
  year: string
  totalIncome: number
  totalExpense: number
  balance: number
  billCount: number
  monthlyDetails: MonthlyStat[]
  categoryBreakdown: CategoryRatio[]
}

/** 预算 */
export interface Budget {
  id: number
  categoryId: number
  categoryName: string
  month: string
  amount: number
  usedAmount: number
}

/** 预算使用情况 */
export interface BudgetUsage {
  totalBudget: number
  totalUsed: number
  items: Budget[]
}

/** 周期账单 */
export interface RecurringBill {
  id: number
  amount: number
  type: number
  typeName: string
  categoryId: number
  categoryName: string
  remark: string
  frequency: string
  dayOfPeriod: number
  startDate: string
  endDate?: string
  lastGeneratedDate?: string
  status: number
  createdAt: string
}
