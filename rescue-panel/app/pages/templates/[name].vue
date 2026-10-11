<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import type { Template, TemplateChannel, TemplateRequest } from '~/types'

const route = useRoute()
const router = useRouter()
const api = useApi()
const toast = useToast()
const confirm = useConfirm()

const name = computed(() => String(route.params.name))

const form = reactive<TemplateRequest>({
  templateName: '',
  description: '',
  channel: 'EMAIL',
  content: '',
})

const loading = ref(true)
const saving = ref(false)

const channelOptions: { label: string; value: TemplateChannel }[] = [
  { label: 'Email', value: 'EMAIL' },
  { label: 'Push', value: 'PUSH' },
  { label: 'SMS', value: 'SMS' },
]

async function load() {
  loading.value = true
  try {
    const template = await api<Template>(
      `/api/v1/templates/${encodeURIComponent(name.value)}`,
    )
    form.templateName = template.templateName
    form.description = template.description ?? ''
    form.channel = template.channel
    form.content = template.content
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Failed to load template',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await api(`/api/v1/templates/${encodeURIComponent(name.value)}`, {
      method: 'PATCH',
      body: {
        templateName: form.templateName.trim(),
        description: form.description?.trim() || null,
        channel: form.channel,
        content: form.content,
      },
    })
    toast.add({ severity: 'success', summary: 'Template updated', life: 2500 })
    await load()
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Update failed',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    saving.value = false
  }
}

function remove() {
  confirm.require({
    message: `Delete template “${name.value}”?`,
    header: 'Confirm delete',
    icon: 'pi pi-trash',
    acceptLabel: 'Delete',
    rejectLabel: 'Cancel',
    accept: async () => {
      try {
        await api(`/api/v1/templates/${encodeURIComponent(name.value)}`, {
          method: 'DELETE',
        })
        toast.add({ severity: 'success', summary: 'Template deleted', life: 2500 })
        await router.push('/templates')
      } catch (e) {
        toast.add({
          severity: 'error',
          summary: 'Delete failed',
          detail: (e as Error).message,
          life: 4000,
        })
      }
    },
  })
}

onMounted(load)
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
      <Button
        label="Delete"
        icon="pi pi-trash"
        severity="danger"
        outlined
        @click="remove"
      />
    </div>

    <div v-if="loading" class="muted"><i class="pi pi-spin pi-spinner" /> Loading…</div>

    <Card v-else>
      <template #title>Edit template</template>
      <template #content>
        <div class="w-full">
          <label class="muted">Name</label>
          <InputText v-model="form.templateName" class="w-full mt-1" />
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
          <Textarea v-model="form.content" class="w-full mt-1" rows="10" autoResize />
        </div>
        <Button
          class="mt-3"
          label="Save changes"
          icon="pi pi-check"
          :loading="saving"
          @click="save"
        />
      </template>
    </Card>
  </div>
</template>
