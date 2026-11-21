Add-Type -AssemblyName System.Drawing
$canvas = 432

function Render-Monogram([string]$path, [bool]$withBackground, [int]$fontPx) {
    # step 1: draw text on a scratch bitmap
    $tmp = New-Object System.Drawing.Bitmap($canvas, $canvas)
    $g1 = [System.Drawing.Graphics]::FromImage($tmp)
    $g1.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g1.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
    $g1.Clear([System.Drawing.Color]::Transparent)

    $font = New-Object System.Drawing.Font("Microsoft YaHei", [float]$fontPx, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $fmt.FormatFlags = [System.Drawing.StringFormatFlags]::NoClip
    $rect = New-Object System.Drawing.RectangleF(0, 0, $canvas, $canvas)

    # gradient: blue on the left -> green on the right
    # diagonal axis (~30 deg from horizontal): left/top is blue, right/bottom is green
    $p1 = New-Object System.Drawing.PointF([float]0, [float]($canvas * 0.15))
    $p2 = New-Object System.Drawing.PointF([float]$canvas, [float]($canvas * 0.85))
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush($p1, $p2, [System.Drawing.Color]::FromArgb(255, 26, 122, 224), [System.Drawing.Color]::FromArgb(255, 70, 224, 140))
    $g1.DrawString("BC", $font, $grad, $rect, $fmt)
    $g1.Dispose()

    # step 2: scan ink bounds
    $minX = $canvas; $maxX = -1; $minY = $canvas; $maxY = -1
    for ($y = 0; $y -lt $canvas; $y++) {
        for ($x = 0; $x -lt $canvas; $x++) {
            if ($tmp.GetPixel($x, $y).A -gt 16) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    $inkW = $maxX - $minX + 1
    $inkH = $maxY - $minY + 1
    Write-Output ("ink box " + $minX + "," + $minY + " - " + $maxX + "," + $maxY + "  size " + $inkW + "x" + $inkH)

    # step 3: final canvas
    $bmp = New-Object System.Drawing.Bitmap($canvas, $canvas)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    if ($withBackground) {
        $r = 96; $d = $r * 2
        $p = New-Object System.Drawing.Drawing2D.GraphicsPath
        $p.AddArc(0, 0, $d, $d, 180, 90)
        $p.AddArc($canvas - $d, 0, $d, $d, 270, 90)
        $p.AddArc($canvas - $d, $canvas - $d, $d, $d, 0, 90)
        $p.AddArc(0, $canvas - $d, $d, $d, 90, 90)
        $p.CloseFigure()
        # base: faint blue -> green wash on the same diagonal axis as the letters
        $bg1 = New-Object System.Drawing.PointF([float]0, [float]0)
        $bg2 = New-Object System.Drawing.PointF([float]$canvas, [float]$canvas)
        $wash = New-Object System.Drawing.Drawing2D.LinearGradientBrush($bg1, $bg2, [System.Drawing.Color]::FromArgb(255, 14, 23, 30), [System.Drawing.Color]::FromArgb(255, 11, 26, 20))
        $g.FillPath($wash, $p)
        # overlay: soft centre lift so the plate is not flat
        $pg = New-Object System.Drawing.Drawing2D.PathGradientBrush($p)
        $c = $canvas / 2
        $pg.CenterPoint = New-Object System.Drawing.PointF([float]$c, [float]$c)
        $pg.CenterColor = [System.Drawing.Color]::FromArgb(70, 52, 66, 61)
        $pg.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 20, 26, 24))
        $g.FillPath($pg, $p)
    }

    $dx = [int][Math]::Round(($canvas - $inkW) / 2 - $minX)
    $dy = [int][Math]::Round(($canvas - $inkH) / 2 - $minY)
    $dest = New-Object System.Drawing.Rectangle($dx, $dy, $canvas, $canvas)
    $g.DrawImage($tmp, $dest)

    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $tmp.Dispose()
    Write-Output ("written " + $path + " shift dx=" + $dx + " dy=" + $dy)
}

Render-Monogram "app/src/main/res/drawable-nodpi/ic_launcher_foreground.png" $false 150
Render-Monogram "app/src/main/res/drawable-nodpi/ic_launcher_full.png" $true 150
