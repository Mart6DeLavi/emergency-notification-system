<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import type { TemplateChannel, TemplateRequest } from '~/types'

const api = useApi()
const toast = useToast()
const router = useRouter()

const form = reactive<TemplateRequest>({
  templateName: '',
  description: '',
  channel: 'EMAIL',
  content: '',
})

const saving = ref(false)

const channelOptions: { label: string; value: TemplateChannel }[] = [
  { label: 'Email', value: 'EMAIL' },
  { label: 'Push', value: 'PUSH' },
  { label: 'SMS', value: 'SMS' },
]

const canSave = computed(
  () => !!form.templateName.trim() && !!form.content.trim() && !!form.channel,
)

async function save() {
  saving.value = true
  try {
    await api('/api/v1/templates', {
      method: 'POST',
      body: {
        templateName: form.templateName.trim(),
        description: form.description?.trim() || null,
        channel: form.channel,
        content: form.content,
      },
    })
    toast.add({ severity: 'success', summary: 'Template created', life: 2500 })
    await router.push('/templates')
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Create failed',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="flex-between mb-2">
      <Button
        label="Back"
        icon="pi pi-arrow-left"
        text
        severity="secondary"
        @click="router.push('/templates')"
      />
    </div>

    <Card>
      <template #title>New template</template>
      <template #content>
        <div class="w-full">
          <label class="muted">Name</label>
          <InputText v-model="form.templateName" class="w-full mt-1" placeholder="emergency-alert" />
        </div>
        <div class="w-full mt-2">
          <label class="muted">Description</label>
          <InputText v-model="form.description" class="w-full mt-1" />
        </div>
        <div class="w-full mt-2">
          <label class="muted">Channel</label>
          <Select
            v-model="form.channel"
            :options="channelOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full mt-1"
          />
        </div>
        <div class="w-full mt-2">
          <label class="muted">Content (supports &#123;&#123;placeholder&#125;&#125; syntax)</label>
          <Textarea
            v-model="form.content"
            class="w-full mt-1"
            rows="8"
            autoResize
            placeholder="⚠️ Emergency alert&#10;{{title}}&#10;{{description}}&#10;Location: {{city}}, {{street}}"
          />
        </div>
        <Button
          class="mt-3"
          label="Create template"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!canSave"
          @click="save"
        />
      </template>
    </Card>
  </div>
</template>
