<template>
  <div class="relative w-full bg-black rounded-lg overflow-hidden" :style="{ aspectRatio: '16/9' }">
    <video ref="videoRef" class="w-full h-full" muted autoplay></video>
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-black/60">
      <div class="text-center text-white">
        <Icon icon="mdi:loading" class="text-3xl animate-spin" />
        <p class="mt-2 text-xs">正在连接视频流...</p>
      </div>
    </div>
    <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-black/80">
      <div class="text-center text-white">
        <Icon icon="mdi:video-off" class="text-3xl text-red-400" />
        <p class="mt-2 text-xs text-red-300">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import mpegts from 'mpegts.js'
import { Icon } from '@iconify/vue'

const props = defineProps<{
  url: string
}>()

const videoRef = ref<HTMLVideoElement>()
const loading = ref(true)
const error = ref('')
let player: mpegts.Player | null = null

function createPlayer() {
  destroyPlayer()
  if (!props.url || !videoRef.value) return

  loading.value = true
  error.value = ''

  if (!mpegts.isSupported()) {
    error.value = '当前浏览器不支持视频播放'
    loading.value = false
    return
  }

  try {
    player = mpegts.createPlayer({
      type: 'flv',
      isLive: true,
      url: props.url,
    }, {
      enableWorker: false,
      enableStashBuffer: false,
      stashInitialSize: 128,
      lazyLoadMaxDuration: 3 * 60,
      seekType: 'range',
    })

    player.attachMediaElement(videoRef.value)
    player.load()
    player.play()

    player.on(mpegts.Events.ERROR, (_type: string, detail: string) => {
      error.value = '视频流连接失败: ' + detail
      loading.value = false
    })

    player.on(mpegts.Events.LOADING_COMPLETE, () => {
      loading.value = false
    })

    // Once data starts arriving, hide loading
    videoRef.value.addEventListener('loadeddata', () => {
      loading.value = false
    }, { once: true })

    // Fallback timeout
    setTimeout(() => { loading.value = false }, 5000)
  } catch (e) {
    error.value = '播放器初始化失败'
    loading.value = false
  }
}

function destroyPlayer() {
  if (player) {
    player.pause()
    player.unload()
    player.detachMediaElement()
    player.destroy()
    player = null
  }
}

onMounted(() => {
  if (props.url) createPlayer()
})

watch(() => props.url, (newUrl) => {
  if (newUrl) createPlayer()
  else destroyPlayer()
})

onBeforeUnmount(() => {
  destroyPlayer()
})
</script>
