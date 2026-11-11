/** 将租赁分类树扁平化为下拉选项（含 id、name） */
export function flattenRentCategories(nodes = [], bucket = []) {
  nodes.forEach((node) => {
    bucket.push({ id: node.id, name: node.name })
    if (node.children?.length) {
      flattenRentCategories(node.children, bucket)
    }
  })
  return bucket
}
