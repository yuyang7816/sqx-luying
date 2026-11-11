/** GoodsEditView 表单校验规则 */
export const GOODS_FORM_RULES = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  coverImage: [{ required: true, message: '请上传封面图', trigger: 'change' }],
  rentPriceDay: [{ required: true, message: '请输入日租价', trigger: 'blur' }],
  depositAmount: [{ required: true, message: '请输入押金', trigger: 'blur' }],
}
