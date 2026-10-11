<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import type { Template } from '~/types'

const api = useApi()
const toast = useToast()
const confirm = useConfirm()
const router = useRouter()

const templates = ref<Template[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    templates.value = await api<Template[]>('/api/v1/templates')
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Failed to load templates',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

function remove(template: Template) {
  confirm.require({
    message: `Delete template “${template.templateName}”?`,
    header: 'Confirm delete',
    icon: 'pi pi-trash',
    acceptLabel: 'Delete',
    rejectLabel: 'Cancel',
    accept: async () => {
      try {
        await api(`/api/v1/templates/${encodeURIComponent(template.templateName)}`, {
          method: 'DELETE',
        })
        toast.add({ severity: 'success', summary: 'Template deleted', life: 2500 })
        await load()
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
      <div>
        <h2 style="margin: 0">Notification Templates</h2>
        <span class="muted">Used when broadcasting confirmed emergencies</span>
      </div>
      <Button
        label="New template"
        icon="pi pi-plus"
        @click="router.push('/templates/new')"
      />
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="templates"
          :loading="loading"
          dataKey="templateName"
          stripedRows
          emptyMessage="No templates yet"
        >
          <Column field="templateName" header="Name" />
          <Column header="Channel">
            <template #body="{ data }">
              <Tag :value="data.channel" />
            </template>
          </Column>
          <Column field="description" header="Description">
            <template #body="{ data }">{{ data.description || '—' }}</template>
          </Column>
          <Column header="Updated">
            <template #body="{ data }">{{ formatDateTime(data.updatedAt) }}</template>
          </Column>
          <Column header="">
            <template #body="{ data }">
              <div class="flex-between" style="justify-content: flex-end; gap: 0.25rem">
                <Button
                  icon="pi pi-pencil"
                  text
                  size="small"
                  @click="router.push(`/templates/${encodeURIComponent(data.templateName)}`)"
                />
                <Button
                  icon="pi pi-trash"
                  text
                  size="small"
                  severity="danger"
                  @click="remove(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>
