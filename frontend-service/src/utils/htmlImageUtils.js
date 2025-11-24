// Replace Moodle @@PLUGINFILE@@ image URLs with base64 data URIs from imagesJson
export function replacePluginImagesWithBase64(html, imagesJson) {
  if (!html) return html;
  if (!imagesJson) return html;
  let images = [];
  try {
    images = JSON.parse(imagesJson);
  } catch (_) {
    return html;
  }
  if (!Array.isArray(images) || images.length === 0) return html;

  const getMimeFromExt = (src) => {
    const match = (src || '').match(/\.([a-zA-Z0-9]+)(?:\?|$)/);
    const ext = match ? match[1].toLowerCase() : '';
    if (ext === 'png') return 'image/png';
    if (ext === 'gif') return 'image/gif';
    if (ext === 'webp') return 'image/webp';
    // default to jpeg (most common from pipeline)
    return 'image/jpeg';
  };

  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const imgNodes = doc.querySelectorAll('img[src^="@@PLUGINFILE@@/"]');
    imgNodes.forEach((node, idx) => {
      const data = images[idx] && (images[idx].img_base64 || images[idx].image_base64 || images[idx].base64);
      if (!data) return;
      const mime = getMimeFromExt(node.getAttribute('src'));
      node.setAttribute('src', `data:${mime};base64,${data}`);
    });
    return doc.body.innerHTML;
  } catch (_) {
    // Fallback: simple sequential replace via regex (less robust)
    let i = 0;
    return html.replace(/<img\b[^>]*src="@@PLUGINFILE@@\/[^"]+"[^>]*>/g, (tag) => {
      const data = images[i] && (images[i].img_base64 || images[i].image_base64 || images[i].base64);
      i += 1;
      if (!data) return tag;
      const mime = getMimeFromExt(tag);
      return tag.replace(/src="[^"]+"/, `src="data:${mime};base64,${data}"`);
    });
  }
}
