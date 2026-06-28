package main

import (
	"image"
	"image/color"
	"image/png"
	"math"
	"os"
)

func main() {
	s := 1024
	img := image.NewRGBA(image.Rect(0, 0, s, s))
	dark := color.RGBA{0x0F, 0x0F, 0x12, 0xFF}
	panel := color.RGBA{0x1C, 0x1C, 0x22, 0xFF}
	amber := color.RGBA{0xF5, 0xA6, 0x23, 0xFF}
	cx, cy := 512.0, 512.0
	half := 452.0 // (1024-120)/2 margin 60
	rr := 215.0
	for y := 0; y < s; y++ {
		for x := 0; x < s; x++ {
			fx, fy := float64(x)+0.5, float64(y)+0.5
			ax := math.Abs(fx-cx) - (half - rr)
			ay := math.Abs(fy-cy) - (half - rr)
			dx := math.Max(ax, 0)
			dy := math.Max(ay, 0)
			if math.Hypot(dx, dy)-rr > 0 {
				continue // outside rounded square -> transparent
			}
			rc := math.Hypot(fx-cx, fy-cy)
			c := dark
			switch {
			case rc < 58:
				c = amber
			case rc < 150:
				c = panel
			case rc < 186:
				c = amber
			case rc < 300:
				c = dark
			case rc < 346:
				c = amber
			}
			img.SetRGBA(x, y, c)
		}
	}
	f, _ := os.Create(os.Args[1])
	png.Encode(f, img)
	f.Close()
}
