with open("color_palette_scheme_defaults.txt") as lines_file:
    lines = lines_file.readlines()

import lxml.etree as ET

root = ET.Element('colorSchemeLut')

for line in lines:
    if not (line.startswith("#") or line.isspace()):
        str_list = line.split(":")
        
        head_branch = ET.SubElement(root, "KEY", REGEX = str_list[0].strip()) 
        
        subhead_branch1 = ET.SubElement(head_branch, "SCHEME_ID")
        subhead_branch1.text = str_list[1].strip()
        
        subhead_branch2 = ET.SubElement(head_branch, "DESCRIPTION")

tree = ET.ElementTree(root)
tree.write("color_palette_scheme_defaults.xml",pretty_print=True)
