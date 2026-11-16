"""Genera el informe actual sin editar los PDF históricos ni los diagramas.

Desde la raíz: python -m pip install reportlab
              python scripts/generar-informe.py
"""
from html import escape
from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, CondPageBreak,
)

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/informe-diseno.md"
OUTPUT = ROOT / "output/pdf/informe-diseno-actualizado.pdf"
INK = colors.HexColor("#163349")
TEAL = colors.HexColor("#137E79")
STYLES = getSampleStyleSheet()
STYLES.add(ParagraphStyle("ReportTitle", fontName="Helvetica-Bold", fontSize=22,
                          leading=26, textColor=INK, spaceAfter=17))
STYLES.add(ParagraphStyle("ReportHeading", fontName="Helvetica-Bold", fontSize=13,
                          leading=17, textColor=TEAL, spaceBefore=13,
                          spaceAfter=8))
STYLES.add(ParagraphStyle("ReportBody", fontName="Helvetica", fontSize=10,
                          leading=14, textColor=INK, spaceAfter=8,
                          splitLongWords=True, alignment=TA_LEFT))
STYLES.add(ParagraphStyle("ReportCell", parent=STYLES["ReportBody"], fontSize=9,
                          leading=12, spaceAfter=0))


def markup(text):
    text = escape(text.replace("`", ""))
    return re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)


def footer(canvas, doc):
    canvas.saveState()
    width, _ = A4
    canvas.setStrokeColor(colors.HexColor("#D7E2E6"))
    canvas.line(19 * mm, 17 * mm, width - 19 * mm, 17 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(INK)
    canvas.drawString(19 * mm, 12 * mm, "SkillSwap | Grupo 03 | Integración PR #65 | 15/11/2026")
    canvas.drawRightString(width - 19 * mm, 12 * mm, str(doc.page))
    canvas.restoreState()


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4,
                            rightMargin=19 * mm, leftMargin=19 * mm,
                            topMargin=19 * mm, bottomMargin=23 * mm,
                            title="SkillSwap - Informe de diseño e integración",
                            author="Grupo 03 - PDS UADE")
    lines = SOURCE.read_text(encoding="utf-8-sig").splitlines()
    story = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", c) for c in cells):
                    rows.append(cells)
                i += 1
            columns = len(rows[0])
            widths = ([doc.width * .25, doc.width * .75] if columns == 2
                      else [doc.width / columns] * columns)
            data = [[Paragraph(markup(c), STYLES["ReportCell"]) for c in r] for r in rows]
            table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E0EEEF")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F4F7F8")]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ("LINEBELOW", (0, 0), (-1, 0), .6, TEAL),
            ]))
            story.extend([table, Spacer(1, 9)])
            continue
        if line.startswith("# "):
            style, text = "ReportTitle", line[2:]
        elif line.startswith("## "):
            story.append(CondPageBreak(85))
            style, text = "ReportHeading", line[3:]
        elif line.startswith("- "):
            style, text = "ReportBody", "• " + line[2:]
        else:
            style, text = "ReportBody", line
            while i + 1 < len(lines) and lines[i + 1].strip() and not lines[i + 1].startswith(("#", "|", "- ")):
                i += 1
                text += " " + lines[i].strip()
        story.append(Paragraph(markup(text), STYLES[style]))
        i += 1
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    build()
