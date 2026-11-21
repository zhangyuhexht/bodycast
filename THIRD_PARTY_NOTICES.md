# Third-party notices

BodyCast itself is released under GPL-3.0 (see `LICENSE`). The third-party code below keeps its own license; the GPL does not replace it.

## Apache-2.0

### Kyant0/AndroidLiquidGlass (Backdrop / liquid glass catalog)

- https://github.com/Kyant0/AndroidLiquidGlass (use the `kmp` branch)
- Copyright 2025-2026 Kyant / Kyant0
- Apache-2.0. Full text in `LICENSE-APACHE-2.0`.
- Files copied or adapted from it, under `app/src/main/java/cn/pocket/toolbox/glass/`:
  `LiquidBottomTab.kt`, `LiquidBottomTabs.kt`, `LiquidButton.kt`, `LiquidToggle.kt`,
  `DampedDragAnimation.kt`, `InteractiveHighlight.kt`
- The original Apache-2.0 headers are still at the top of those files. Nothing was stripped or rewritten.

### Kyant0/Shapes (`io.github.kyant0:shapes-android:1.2.1`)

- https://github.com/Kyant0/Shapes
- Apache-2.0, confirmed from the Maven POM's `<licenses>` section.
- Pulled in by Gradle. No source copied into this repo.

### Other Apache-2.0 dependencies

Jetpack Compose, AndroidX, Activity, AppCompat, Material, Health Connect, Kotlin, AGP and similar libraries are all Gradle dependencies. None of their source was copied here.

Apache-2.0 and GPLv3 are compatible in one direction: Apache-2.0 code may be shipped as part of a GPLv3 work, provided the original copyright, license and disclaimer notices are kept and the full Apache-2.0 text is included. That's what this repo does.

## MIT

### better-mi-fitness-sync

- https://github.com/ilyasaftr/better-mi-fitness-sync
- Copyright (c) 2026 Ilyasa Fathur Rahman
- MIT.
- Ported/adapted into `app/src/main/java/cn/pocket/toolbox/data/micloud/` and
  `app/src/main/java/cn/pocket/toolbox/data/MiCloudSource.kt`.
- The original text is kept in `app/src/main/java/cn/pocket/toolbox/data/micloud/MiLicense.kt`
  and shown in the app under About -> Open source licenses.

MIT License

Copyright (c) 2026 Ilyasa Fathur Rahman

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
