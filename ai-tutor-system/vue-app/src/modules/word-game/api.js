import request from '@/utils/request'

export async function fetchPackages(search = '') {
  const query = search ? `?search=${encodeURIComponent(search)}` : ''
  const res = await request.get(`/api/word-game/packages${query}`)
  return res.data || []
}

export async function recordPackageClick(packageId) {
  await request.post(`/api/word-game/packages/${encodeURIComponent(packageId)}/click`)
}

export async function fetchPackageCourses(packageId) {
  const res = await request.get(`/api/word-game/packages/${encodeURIComponent(packageId)}/courses`)
  return res.data || []
}

export async function fetchCourseQuestions(courseIndex, packageId = '') {
  const query = packageId ? `?packageId=${encodeURIComponent(packageId)}` : ''
  const res = await request.get(`/api/word-game/courses/${courseIndex}/questions${query}`)
  return res.data || []
}

export async function fetchProgress(packageId) {
  const res = await request.get(`/api/word-game/progress?packageId=${encodeURIComponent(packageId)}`)
  return res.data || {}
}

export async function saveProgress(payload) {
  await request.post('/api/word-game/progress', payload)
}

export async function createPackage(payload) {
  const res = await request.post('/api/word-game/packages', payload)
  return res.data
}

export async function addPackageSection(packageId, payload) {
  const res = await request.post(`/api/word-game/packages/${encodeURIComponent(packageId)}/sections`, payload)
  return res.data
}
