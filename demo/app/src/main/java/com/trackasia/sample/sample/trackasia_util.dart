import 'dart:math';
import 'dart:ui' as _ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class TrackasiaUtils {
  static Future<Uint8List> svgToBytes(String assetPath) async {
    final byteData = await rootBundle.load(assetPath);
    return byteData.buffer.asUint8List();
  }

  static final List<Color> colors = [
    const Color(0xFF00ff99),
    const Color(0xFF00b4d8),
    const Color(0xFFFFA500),
    const Color(0xFF006600),
    const Color(0xFFFF4500),
    const Color(0xFF0304af),
  ];

  static Future<Uint8List?> createDonutChartPng(int clusterSize, {double width = 70, double height = 70, double strokeWidth = 12}) async {
    final double radius = min(width, height) / 2 - strokeWidth / 2;

    final _ui.PictureRecorder pictureRecorder = _ui.PictureRecorder();
    final _ui.Canvas canvas = _ui.Canvas(pictureRecorder);
    final _ui.Offset center = _ui.Offset(width / 2, height / 2);

    Map<Color, double> segments = getClusterSegments(clusterSize);

    double startAngle = -pi / 2;
    segments.forEach((color, percentage) {
      final _ui.Paint paint = _ui.Paint()
        ..color = color
        ..style = _ui.PaintingStyle.stroke
        ..strokeWidth = strokeWidth;

      final double sweepAngle = 2 * pi * percentage;
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        startAngle,
        sweepAngle,
        false,
        paint,
      );
      startAngle += sweepAngle;
    });

    final _ui.Image image = await pictureRecorder.endRecording().toImage(width.toInt(), height.toInt());
    final ByteData? byteData = await image.toByteData(format: _ui.ImageByteFormat.png);
    return byteData?.buffer.asUint8List();
  }

  static Map<Color, double> getClusterSegments(int clusterSize) {
    Map<Color, double> segments = {};

    // Xác định số lượng phân đoạn dựa trên kích thước của cluster
    if (clusterSize < 10) {
      segments[colors[0]] = 1.0; // Một màu duy nhất cho cluster nhỏ
    } else if (clusterSize < 50) {
      segments[colors[0]] = 0.3;
      segments[colors[1]] = 0.7;
    } else if (clusterSize < 100) {
      segments[colors[0]] = 0.25;
      segments[colors[1]] = 0.35;
      segments[colors[2]] = 0.4;
    } else if (clusterSize < 200) {
      segments[colors[0]] = 0.2;
      segments[colors[1]] = 0.3;
      segments[colors[2]] = 0.5;
    } else if (clusterSize < 500) {
      segments[colors[0]] = 0.35;
      segments[colors[1]] = 0.25;
      segments[colors[2]] = 0.3;
      segments[colors[3]] = 0.1;
    } else if (clusterSize < 1000) {
      segments[colors[0]] = 0.25;
      segments[colors[1]] = 0.2;
      segments[colors[2]] = 0.25;
      segments[colors[3]] = 0.2;
      segments[colors[4]] = 0.1;
    } else if (clusterSize < 2000) {
      segments[colors[0]] = 0.35;
      segments[colors[1]] = 0.15;
      segments[colors[2]] = 0.2;
      segments[colors[3]] = 0.25;
      // segments[colors[4]] = 0.15;
      segments[colors[5]] = 0.05;
    } else if (clusterSize < 2500) {
      segments[colors[0]] = 0.4;
      segments[colors[1]] = 0.15;
      segments[colors[2]] = 0.15;
      segments[colors[3]] = 0.2;
      // segments[colors[4]] = 0.2;
      segments[colors[5]] = 0.1;
    } else if (clusterSize < 5000) {
      segments[colors[0]] = 0.5;
      segments[colors[1]] = 0.1;
      segments[colors[2]] = 0.15;
      segments[colors[3]] = 0.15;
      // segments[colors[4]] = 0.25;
      segments[colors[5]] = 0.1;
    } else if (clusterSize < 10000) {
      segments[colors[0]] = 0.5;
      segments[colors[1]] = 0.25;
      segments[colors[2]] = 0.10;
      segments[colors[3]] = 0.10;
      // segments[colors[4]] = 0.25;
      segments[colors[5]] = 0.05;
    } else {
      // 10000 trở lên
      segments[colors[0]] = 0.5;
      segments[colors[1]] = 0.25;
      segments[colors[2]] = 0.1;
      segments[colors[3]] = 0.1;
      // segments[colors[4]] = 0.2;
      segments[colors[5]] = 0.05;
    }

    return segments;
  }
}
