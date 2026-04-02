<template>
  <el-dialog
    :model-value="visible"
    :title="bill ? '编辑账单' : '手动记账'"
    width="480px"
    @update:model-value="$emit('update:visible', $event)"
    @closed="resetForm"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="金额" prop="amount">
        <el-input-number
          v-model="form.amount"
          :min="0.01"
          :precision="2"
          :step="1"
          style="width:100%"
          placeholder="请输入金额"
        />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio-button :value="2">支出</el-radio-button>
          <el-radio-button :value="1">收入</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
          <el-option
            v-for="c in categoryOptions"
            :key="c.id"
            :label="(c.icon || '') + ' ' + c.name"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="日期" prop="billDate">
        <el-date-picker
          v-model="form.billDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          style="width:100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { getCategoryList } from '../api/category'
import { useBillStore } from '../stores/bill'
import { getToday } from '../utils'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  bill: Object
})
const emit = defineEmits(['update:visible', 'success'])

const billStore = useBillStore()
const formRef = ref()
const saving = ref(false)
const allCategories = ref([])

const form = reactive({
  amount: null,
  type: 2,
  categoryId: null,
  billDate: getToday(),
  remark: ''
})

const rules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  billDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const categoryOptions = computed(() => {
  return allCategories.value.filter(c => c.type === form.type)
})

watch(() => props.visible, async (val) => {
  if (val) {
    const [expense, income] = await Promise.all([
      getCategoryList(2),
      getCategoryList(1)
    ])
    allCategories.value = [...(expense || []), ...(income || [])]
    if (props.bill) {
      form.amount = props.bill.amount
      form.type = props.bill.type
      form.categoryId = props.bill.categoryId
      form.billDate = props.bill.billDate
      form.remark = props.bill.remark
    }
  }
})

function resetForm() {
  form.amount = null
  form.type = 2
  form.categoryId = null
  form.billDate = getToday()
  form.remark = ''
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (props.bill) {
      await billStore.editBill(props.bill.id, form)
      ElMessage.success('更新成功')
    } else {
      await billStore.addBill(form)
      ElMessage.success('记账成功')
    }
    emit('success')
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>
