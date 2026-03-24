const IMAGE_META_PREFIX = 'IMG_META_JSON:'
const IMAGE_META_BASE64_PREFIX = 'IMG_META:'

const fromBase64 = (value) => {
  const binary = atob(value)
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export const buildImageMeta = (images) => {
  if (!Array.isArray(images) || images.length === 0) return null
  const safeImages = images.filter((item) => typeof item === 'string' && item.trim().length > 0)
  if (safeImages.length === 0) return null
  const payload = JSON.stringify({ images: safeImages })
  return `${IMAGE_META_PREFIX}${payload}`
}

export const parseImageMeta = (raw) => {
  if (typeof raw !== 'string') return []
  try {
    let parsed = null
    if (raw.startsWith(IMAGE_META_PREFIX)) {
      parsed = JSON.parse(raw.slice(IMAGE_META_PREFIX.length))
    } else if (raw.startsWith(IMAGE_META_BASE64_PREFIX)) {
      const encoded = raw.slice(IMAGE_META_BASE64_PREFIX.length)
      const decoded = fromBase64(encoded)
      parsed = JSON.parse(decoded)
    } else {
      return []
    }
    if (!Array.isArray(parsed.images)) return []
    return parsed.images.filter((item) => typeof item === 'string' && item.trim().length > 0)
  } catch {
    return []
  }
}
