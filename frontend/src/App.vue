<template>
  <n-config-provider :theme="darkTheme" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { darkTheme, type GlobalThemeOverrides } from 'naive-ui'
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#8ff5ff',
    primaryColorHover: '#00deec',
    primaryColorPressed: '#00eefc',
    bodyColor: '#060e20',
    cardColor: '#0f1930',
    modalColor: '#0f1930',
    popoverColor: '#141f38',
    tableColor: '#0f1930',
    inputColor: '#091328',
    actionColor: '#091328',
    borderColor: '#40485d26',
    dividerColor: '#40485d26',
    textColorBase: '#dee5ff',
    textColor1: '#dee5ff',
    textColor2: '#a3aac4',
    textColor3: '#6d758c',
  },
}
</script>
