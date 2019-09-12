import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:ui' as ui;

/// 气温折线图
class TempLineWidget extends StatelessWidget {
  final List<Temp> tempList;

  TempLineWidget(this.tempList);

  @override
  Widget build(BuildContext context) {
    return CustomPaint(size: Size(0, 140), painter: TempLinePainter(tempList));
  }
}

Paint maxLinePaint = new Paint()
  ..style = PaintingStyle.stroke
  ..color = Colors.white70
  ..strokeWidth = 1.4;

Paint minLinePaint = new Paint()
  ..style = PaintingStyle.stroke
  ..color = Colors.white54
  ..strokeWidth = 1.4;

Paint dotPaint = new Paint()
  ..style = PaintingStyle.fill
  ..color = Colors.white;

Gradient gradient = new LinearGradient(
  begin: Alignment.topCenter,
  end: Alignment.bottomCenter,
  colors: [
    Colors.white30,
    Colors.transparent,
  ],
);

Rect arcRect = Rect.fromLTRB(0, 0, 0, 140);

Paint bgPaint = new Paint()
  ..style = PaintingStyle.fill
  ..shader = gradient.createShader(arcRect);

class TempLinePainter extends CustomPainter {
  List<Temp> tempList;

  TempLinePainter(this.tempList);

  int margin = 0;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.translate(0, size.height / 2);

    double total = 0;
    tempList.forEach((temp) {
      total += (temp.max + temp.min);
    });
    double average = total / (tempList.length * 2);

    List<Temp> drawList = List();

    tempList.forEach((temp) {
      drawList.add(Temp((average - temp.max) * 5, (average - temp.min) * 5));
    });

    double distance = size.width / tempList.length;

    // 画线
    drawLine(drawList, distance, canvas);

    // 画点和文字
    drawDotText(drawList, distance, canvas);

    // 画背景颜色
    drawBg(drawList, distance, size, canvas);
  }

  void drawLine(List<Temp> dots, double distance, Canvas canvas) {
    for (int i = 0; i < dots.length - 1; i++) {
      double x = distance * i + distance / 2;

      canvas.drawLine(Offset(x, dots.elementAt(i).max),
          Offset(x + distance, dots.elementAt(i + 1).max), maxLinePaint);

      canvas.drawLine(
          Offset(x, dots.elementAt(i).min + margin),
          Offset(x + distance, dots.elementAt(i + 1).min + margin),
          minLinePaint);

      if (i == 0) {
        canvas.drawLine(Offset(0, dots.elementAt(i).max),
            Offset(x, dots.elementAt(i).max), maxLinePaint);

        canvas.drawLine(Offset(0, dots.elementAt(i).min + margin),
            Offset(x, dots.elementAt(i).min + margin), minLinePaint);
      } else if (i == dots.length - 2) {
        canvas.drawLine(
            Offset(x + distance, dots.elementAt(i + 1).max),
            Offset(x + distance + distance / 2, dots.elementAt(i + 1).max),
            maxLinePaint);

        canvas.drawLine(
            Offset(x + distance, dots.elementAt(i + 1).min + margin),
            Offset(x + distance + distance / 2,
                dots.elementAt(i + 1).min + margin),
            minLinePaint);
      }
    }
  }

  void drawDotText(List<Temp> dots, double distance, Canvas canvas) {
    for (int i = 0; i < dots.length; i++) {
      double x = distance * i + distance / 2;

      // 画点
      canvas.drawCircle(Offset(x, dots.elementAt(i).max), 2, dotPaint);
      canvas.drawCircle(Offset(x, dots.elementAt(i).min + margin), 2, dotPaint);

      // 画文字
      ParagraphBuilder pb = ParagraphBuilder(ParagraphStyle(
          textAlign: TextAlign.left,
          fontWeight: FontWeight.w500,
          fontStyle: FontStyle.normal,
          fontSize: 12));
      pb.pushStyle(ui.TextStyle(color: Colors.white70));

      pb.addText('${tempList.elementAt(i).max.toInt()}' + '°');
      ParagraphConstraints pc = ParagraphConstraints(width: 30);
      Paragraph paragraph = pb.build()..layout(pc);
      Offset offset = Offset(x, dots.elementAt(i).max - 20);
      canvas.drawParagraph(paragraph, offset);

      pb.addText('${tempList.elementAt(i).min.toInt()}' + '°');
      Paragraph paragraph2 = pb.build()..layout(pc);
      Offset offset2 = Offset(x, dots.elementAt(i).min + margin + 5);

      canvas.drawParagraph(paragraph2, offset2);
    }
  }

  void drawBg(List<Temp> dots, double distance, Size size, Canvas canvas) {
    Path path = new Path();
    path.moveTo(0, dots.elementAt(0).max);
    for (int i = 0; i < dots.length; i++) {
      double x = distance * i + distance / 2;
      path.lineTo(x, dots.elementAt(i).max);
    }

    path.lineTo(size.width, dots.elementAt(5).max);
    path.lineTo(size.width, dots.elementAt(5).min + margin);

    for (int i = 0; i < dots.length; i++) {
      double x = distance * (5 - i) + distance / 2;
      path.lineTo(x, dots.elementAt(5 - i).min + margin);
    }

    path.lineTo(0, dots.elementAt(0).min + margin);
    path.close();
    canvas.drawPath(path, bgPaint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}

class Temp {
  double max;

  double min;

  Temp(this.max, this.min);
}