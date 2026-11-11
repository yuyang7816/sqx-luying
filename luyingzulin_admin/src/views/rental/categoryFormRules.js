/** CategoryView 表单校验规则（与 Element Plus el-form 共用） */
export const CATEGORY_FORM_RULES = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}
