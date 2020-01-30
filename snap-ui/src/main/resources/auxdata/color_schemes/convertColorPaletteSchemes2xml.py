with open("color_palette_schemes.txt") as lines_file:
    lines = lines_file.readlines()

import lxml.etree as ET

root = ET.Element('schemeList')

for line in lines:
    if not (line.startswith("#") or line.isspace()):
        id, min, max, logScale, cpdFileName, cpdFilenameColorBlind, colorBarTtitle, ColorBarLabels, description = line.split(":")
        
        head_branch = ET.SubElement(root, "Scheme", name=id.strip())

        subhead_branch1 = ET.SubElement(head_branch, "DISPLAY_NAME")

        subhead_branch2 = ET.SubElement(head_branch, "MIN")
        subhead_branch2.text = min.strip()
        
        subhead_branch3 = ET.SubElement(head_branch, "MAX")
        subhead_branch3.text = max.strip()
        
        subhead_branch4 = ET.SubElement(head_branch, "LOG_SCALE")
        subhead_branch4.text = logScale.strip()
        
        subhead_branch5 = ET.SubElement(head_branch, "CPD_FILENAME")
        subhead_branch5.text = cpdFileName.strip()
        
        subhead_branch6 = ET.SubElement(head_branch, "CPD_FILENAME_COLORBLIND")
        subhead_branch6.text = cpdFilenameColorBlind.strip()
        
        subhead_branch7 = ET.SubElement(head_branch, "COLORBAR_TITLE")
        subhead_branch7.text = colorBarTtitle.strip()
        
        subhead_branch8 = ET.SubElement(head_branch, "COLORBAR_LABELS")
        subhead_branch8.text = ColorBarLabels.strip()
        
        subhead_branch9 = ET.SubElement(head_branch, "DESCRIPTION")
        subhead_branch9.text = description.strip()

tree = ET.ElementTree(root)
tree.write("color_palette_schemes.xml",pretty_print=True)
