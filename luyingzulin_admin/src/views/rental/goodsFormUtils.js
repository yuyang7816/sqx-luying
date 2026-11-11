/** 详情接口数据 → 编辑表单 */
export function detailToForm(detail = {}) {
  return {
    id: detail.id ?? null,
    name: detail.name ?? '',
    subtitle: detail.subtitle ?? '',
    categoryId: detail.categoryId ?? null,
    detail: detail.detail ?? '',
    coverImage: detail.coverImage ?? '',
    tutorialVideoUrl: detail.tutorialVideoUrl ?? '',
    rentPriceDay: detail.rentPriceDay ?? null,
    rentPriceHour: detail.rentPriceHour ?? null,
    rentPriceOvernight: detail.rentPriceOvernight ?? null,
    depositAmount: detail.depositAmount ?? null,
    tags: Array.isArray(detail.tags) ? [...detail.tags] : [],
    sort: detail.sort ?? 0,
    status: detail.status ?? 0,
    merchantId: detail.merchantId ?? null,
    totalStock: detail.totalStock ?? 0,
    images: Array.isArray(detail.images) ? [...detail.images] : [],
    specs: [],
    params: (detail.params || detail.specs || []).map((item, index) => ({
      paramName: item.name ?? item.paramName ?? item.specName ?? '',
      paramValue: item.value ?? item.paramValue ?? item.specValue ?? '',
      sort: item.sort ?? index + 1,
    })),
    packages: (detail.packages || []).map((item, index) => ({
      packageName: item.packageName ?? '',
      packageDesc: item.packageDesc ?? '',
      price: item.price ?? 0,
      sort: item.sort ?? index + 1,
    })),
  }
}

/** 编辑表单 → 保存/更新入参；编辑时不提交 status，上下架在列表页操作 */
export function formToPayload(form, { includeStatus = true } = {}) {
  const params = (form.params || [])
    .filter((item) => item.paramName?.trim() && item.paramValue?.trim())
    .map((item, index) => ({
      specName: item.paramName.trim(),
      specValue: item.paramValue.trim(),
      specType: 2,
      sort: item.sort ?? index + 1,
    }))

  const packages = (form.packages || [])
    .filter((item) => item.packageName?.trim())
    .map((item, index) => ({
      packageName: item.packageName.trim(),
      packageDesc: item.packageDesc?.trim() || undefined,
      price: item.price ?? 0,
      sort: item.sort ?? index + 1,
    }))

  const payload = {
    name: form.name?.trim(),
    subtitle: form.subtitle?.trim() || undefined,
    categoryId: form.categoryId,
    detail: form.detail || undefined,
    coverImage: form.coverImage || undefined,
    tutorialVideoUrl: form.tutorialVideoUrl?.trim() || '',
    rentPriceDay: form.rentPriceDay,
    rentPriceHour: form.rentPriceHour ?? undefined,
    rentPriceOvernight: form.rentPriceOvernight ?? undefined,
    depositAmount: form.depositAmount,
    tags: form.tags?.length ? form.tags : undefined,
    sort: form.sort ?? 0,
    merchantId: form.merchantId ?? undefined,
    totalStock: form.totalStock ?? undefined,
    images: form.images?.length ? form.images : undefined,
    specs: undefined,
    params: params.length ? params : undefined,
    packages: packages.length ? packages : undefined,
  }

  if (includeStatus) {
    payload.status = form.status ?? 0
  }

  return payload
}

export function createEmptyGoodsForm() {
  return detailToForm({})
}
