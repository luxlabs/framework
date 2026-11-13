#!/usr/bin/env python3
# REM F2d, item I21: static check of the compiled Valo theme.
# The top border of Table/TreeTable body cells must not make rows taller than
# the header row height: body cell content height = row height + border width,
# body cells use border-box, the first row has the plain row height.
# Usage: python3 -I check-i21-css.py <vaadin-themes jar or VAADIN/themes/valo/styles.css>
# Exit 0 if the rules match, 1 otherwise.
import re
import sys
import zipfile

ROW = 37      # $v-unit-size of Valo
BORDER = 1    # first-number($v-border) of Valo
BODY = ROW + BORDER


def load(path):
    if path.endswith('.jar'):
        with zipfile.ZipFile(path) as jar:
            return jar.read('VAADIN/themes/valo/styles.css').decode('utf-8')
    with open(path, encoding='utf-8') as f:
        return f.read()


def rules(css):
    css = re.sub(r'/\*.*?\*/', '', css, flags=re.S)
    found = {}
    for m in re.finditer(r'([^{}]+)\{([^{}]*)\}', css):
        body = ' '.join(m.group(2).split())
        for sel in ' '.join(m.group(1).split()).split(','):
            found.setdefault(sel.strip(), []).append(body)
    return found


def main():
    found = rules(load(sys.argv[1]))
    expected = [
        ('.valo .v-table-cell-content', 'height: %dpx;' % BODY),
        ('.valo .v-table-table td', 'box-sizing: border-box;'),
        ('.valo .v-table-table tr:first-child > td', 'height: %dpx;' % ROW),
        ('.valo .v-table-row-drag-bottom td.v-table-cell-content', 'height: %dpx;' % (BODY - 2)),
        ('.valo .v-table-row-drag-top td.v-table-cell-content', 'height: %dpx;' % (BODY - 2 + BORDER)),
    ]
    ok = True
    for sel, decl in expected:
        bodies = found.get(sel, [])
        hit = any(decl in b for b in bodies)
        ok = ok and hit
        print('%s %s { %s } observed: %s' % ('OK  ' if hit else 'FAIL', sel, decl, ' | '.join(bodies) or 'missing'))
    print('I21 compiled CSS check:', 'PASS' if ok else 'FAIL')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
