<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [Number, String, Boolean], default: undefined },
  activeValue: { type: [Number, String, Boolean], default: 1 },
  inactiveValue: { type: [Number, String, Boolean], default: 0 },
  /** 多值状态时：命中视为开启 */
  activeValues: { type: Array, default: null },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['change'])

const isOn = computed(() => {
  if (props.activeValues?.length) {
    return props.activeValues.includes(props.modelValue)
  }
  return props.modelValue === props.activeValue
})

function handleChange(checked) {
  if (props.disabled || props.readonly) return
  emit('change', checked ? props.activeValue : props.inactiveValue)
}
</script>

<template>
  <el-switch
    :model-value="isOn"
    :disabled="disabled || readonly"
    @change="handleChange"
  />
</template>
