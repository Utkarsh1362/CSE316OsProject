/**
 * FSROT — File System Recovery & Optimization Tool
 * File: FSROT_GUI.java
 * Simulation only. No real disk operations performed.
 */
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

public class FSROT_GUI extends JFrame {

    // ── palette ──────────────────────────────────────────────────────────────
    static final Color BG        = new Color(10, 12, 16);
    static final Color SURFACE   = new Color(18, 22, 30);
    static final Color SURFACE2  = new Color(26, 32, 44);
    static final Color BORDER    = new Color(40, 50, 68);
    static final Color ACCENT    = new Color(56, 189, 248);
    static final Color GREEN     = new Color(52, 211, 153);
    static final Color RED       = new Color(248, 113, 113);
    static final Color AMBER     = new Color(251, 191, 36);
    static final Color PURPLE    = new Color(167, 139, 250);
    static final Color TEXT      = new Color(226, 232, 240);
    static final Color TEXT_DIM  = new Color(100, 116, 139);
    static final Color TEXT_HINT = new Color(51, 65, 85);
    static final Font  MONO      = new Font("Monospaced", Font.PLAIN, 12);
    static final Font  MONO_B    = new Font("Monospaced", Font.BOLD, 12);
    static final Font  SANS      = new Font("SansSerif", Font.PLAIN, 13);
    static final Font  SANS_B    = new Font("SansSerif", Font.BOLD, 13);
    static final Font  TITLE_F   = new Font("SansSerif", Font.BOLD, 11);

    // ── simulation state ─────────────────────────────────────────────────────
    static final int TOTAL_BLOCKS = 128;
    static final int BLOCK_SIZE   = 512;

    enum BlockState { FREE, USED, CORRUPTED, RECOVERED }

    static class SimBlock {
        int id; BlockState state = BlockState.FREE; String data = "";
        SimBlock(int id) { this.id = id; }
    }

    static class SimInode {
        int id; String name; boolean isDir; int parentId;
        int size; List<Integer> blocks = new ArrayList<>();
        boolean deleted = false; boolean recovered = false;
        SimInode(int id, String name, boolean isDir, int parentId, int size) {
            this.id=id; this.name=name; this.isDir=isDir;
            this.parentId=parentId; this.size=size;
        }
    }

    SimBlock[]              disk        = new SimBlock[TOTAL_BLOCKS];
    List<SimInode>          inodes      = new CopyOnWriteArrayList<>();
    List<String>            sysLog      = new CopyOnWriteArrayList<>();
    List<String>            journal     = new CopyOnWriteArrayList<>();
    List<String>            recoveryLog = new CopyOnWriteArrayList<>();
    Map<Integer,String[]>   backup      = new HashMap<>();
    int nextId = 0;
    int fragmentation = 0;
    int recoveredCount = 0;
    int readOps = 0, writeOps = 0;

    // ── UI components ─────────────────────────────────────────────────────────
    DiskMapPanel   diskMap;
    JTextArea      logArea;
    JTextArea      journalArea;
    JTextArea      recLogArea;
    JTree          fileTree;
    DefaultTreeModel treeModel;
    DefaultMutableTreeNode treeRoot;
    MetricCard     mcUsed, mcFree, mcCorrupt, mcRecovery;
    JLabel         fragLabel;
    JProgressBar   usageBar, fragBar;
    JTextArea      schedArea;
    JTextArea      defragArea;

    // ─────────────────────────────────────────────────────────────────────────
    public FSROT_GUI() {
        super("FSROT — File System Recovery & Optimization Tool");
        for (int i = 0; i < TOTAL_BLOCKS; i++) disk[i] = new SimBlock(i);
        initUI();
        seedInitialFS();
        refreshAll();
        setVisible(true);
    }

    // ── master layout ─────────────────────────────────────────────────────────
    void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setBackground(BG);

        root.add(buildTopBar(),      BorderLayout.NORTH);
        root.add(buildLeftPanel(),   BorderLayout.WEST);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildRightPanel(),  BorderLayout.EAST);
        root.add(buildBottomBar(),   BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── top bar ───────────────────────────────────────────────────────────────
    JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(0,0,1,0,BORDER));
        bar.setPreferredSize(new Dimension(0, 48));

        JLabel logo = new JLabel("  ◈ FSROT");
        logo.setFont(new Font("Monospaced", Font.BOLD, 15));
        logo.setForeground(ACCENT);

        JLabel sub = new JLabel("File System Recovery & Optimization Tool  ");
        sub.setFont(MONO);
        sub.setForeground(TEXT_DIM);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        left.setOpaque(false);
        left.add(logo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);
        right.add(sub);
        right.add(makePill("v1.0", ACCENT));
        right.add(makePill("JAVA SWING", PURPLE));

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── left panel: file tree + metrics ──────────────────────────────────────
    JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12,12,12,6));
        p.setPreferredSize(new Dimension(260, 0));

        // metric cards
        JPanel cards = new JPanel(new GridLayout(2,2,8,8));
        cards.setOpaque(false);
        mcUsed     = new MetricCard("USED",      "0",   ACCENT);
        mcFree     = new MetricCard("FREE",      "128", GREEN);
        mcCorrupt  = new MetricCard("CORRUPT",   "0",   RED);
        mcRecovery = new MetricCard("RECOVERED", "0",   PURPLE);
        cards.add(mcUsed); cards.add(mcFree);
        cards.add(mcCorrupt); cards.add(mcRecovery);

        // file tree
        treeRoot = new DefaultMutableTreeNode("/ (root)");
        treeModel = new DefaultTreeModel(treeRoot);
        fileTree = new JTree(treeModel);
        fileTree.setBackground(SURFACE);
        fileTree.setForeground(TEXT);
        fileTree.setFont(MONO);
        fileTree.setRowHeight(22);
        fileTree.setBorder(new EmptyBorder(4,4,4,4));
        fileTree.setOpaque(true);
        styleTree(fileTree);

        JScrollPane treeScroll = darkScroll(fileTree);

        JPanel treeCard = panel(SURFACE, new BorderLayout());
        treeCard.setBorder(cardBorder("FILE TREE"));
        treeCard.add(treeScroll);

        // usage bars
        JPanel bars = panel(SURFACE, new GridLayout(4,1,0,4));
        bars.setBorder(cardBorder("DISK STATUS"));
        usageBar = makeBar(ACCENT, 0);
        fragBar  = makeBar(AMBER, 0);
        fragLabel = dimLabel("Fragmentation: 0%");
        bars.add(barRow("Usage", usageBar));
        bars.add(barRow("Frag",  fragBar));

        p.add(cards,    BorderLayout.NORTH);
        p.add(treeCard, BorderLayout.CENTER);
        p.add(bars,     BorderLayout.SOUTH);
        return p;
    }

    // ── center panel: disk map + controls ────────────────────────────────────
    JPanel buildCenterPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 6, 12, 6));

        // disk map
        diskMap = new DiskMapPanel();
        JPanel mapCard = panel(SURFACE, new BorderLayout());
        mapCard.setBorder(cardBorder("DISK BLOCK MAP  [ " + TOTAL_BLOCKS + " blocks × " + BLOCK_SIZE + "B ]"));
        mapCard.add(diskMap);
        mapCard.add(buildDiskLegend(), BorderLayout.SOUTH);

        // action buttons
        JPanel actions = panel(SURFACE, new GridLayout(2,4,8,8));
        actions.setBorder(cardBorder("ACTIONS"));
        actions.add(actionBtn("＋ Create File",  ACCENT,  this::doCreateFile));
        actions.add(actionBtn("＋ Create Dir",   ACCENT,  this::doCreateDir));
        actions.add(actionBtn("✕ Delete Random", RED,     this::doDeleteRandom));
        actions.add(actionBtn("⟳ Read File",     GREEN,   this::doReadFile));
        actions.add(actionBtn("💾 Take Backup",  PURPLE,  this::doBackup));
        actions.add(actionBtn("⚡ Crash Disk",   RED,     this::doCrash));
        actions.add(actionBtn("♻ Recover",       GREEN,   this::doRecover));
        actions.add(actionBtn("▶ Full Demo",     AMBER,   this::doFullDemo));

        // scheduler panel
        schedArea = darkTextArea();
        schedArea.setText("Click 'Compare Schedulers' to run FCFS / SSTF / SCAN analysis.");
        JPanel schedCard = panel(SURFACE, new BorderLayout());
        schedCard.setBorder(cardBorder("DISK SCHEDULER"));
        schedCard.add(darkScroll(schedArea));
        JButton schedBtn = actionBtn("⚙ Compare Schedulers", ACCENT, this::doScheduler);
        schedCard.add(schedBtn, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new GridLayout(1,2,12,0));
        bottom.setOpaque(false);
        bottom.add(actions);
        bottom.add(schedCard);

        p.add(mapCard, BorderLayout.CENTER);
        p.add(bottom,  BorderLayout.SOUTH);
        bottom.setPreferredSize(new Dimension(0, 200));
        return p;
    }

    // ── right panel: logs ─────────────────────────────────────────────────────
    JPanel buildRightPanel() {
        JPanel p = new JPanel(new GridLayout(3,1,0,12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12,6,12,12));
        p.setPreferredSize(new Dimension(300, 0));

        logArea     = darkTextArea();
        journalArea = darkTextArea();
        recLogArea  = darkTextArea();

        JPanel logCard = panel(SURFACE, new BorderLayout());
        logCard.setBorder(cardBorder("SYSTEM LOG"));
        logCard.add(darkScroll(logArea));
        JButton clearBtn = smallBtn("clear", () -> { sysLog.clear(); logArea.setText(""); });
        logCard.add(clearBtn, BorderLayout.SOUTH);

        JPanel jCard = panel(SURFACE, new BorderLayout());
        jCard.setBorder(cardBorder("JOURNAL"));
        jCard.add(darkScroll(journalArea));

        JPanel rCard = panel(SURFACE, new BorderLayout());
        rCard.setBorder(cardBorder("RECOVERY LOG"));
        rCard.add(darkScroll(recLogArea));

        p.add(logCard);
        p.add(jCard);
        p.add(rCard);
        return p;
    }

    // ── bottom bar ────────────────────────────────────────────────────────────
    JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(1,0,0,0,BORDER));
        bar.setPreferredSize(new Dimension(0,30));

        JLabel hint = new JLabel("  ◈ Simulation mode — no real disk operations performed");
        hint.setFont(MONO);
        hint.setForeground(TEXT_HINT);

        defragArea = darkTextArea();
        defragArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JButton defBtn = actionBtn("◈ Defragment", GREEN, this::doDefrag);
        defBtn.setFont(MONO);
        defBtn.setPreferredSize(new Dimension(140,20));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(dimLabel("Reads: "));
        right.add(statLabel("0", "readOpsLabel"));
        right.add(dimLabel("  Writes: "));
        right.add(statLabel("0", "writeOpsLabel"));
        right.add(defBtn);

        bar.add(hint,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── simulation actions ────────────────────────────────────────────────────
    void doCreateFile() {
        String[] names    = {"report.txt","readme.md","config.yml","data.csv","app.java","index.html","notes.txt","schema.sql"};
        String[] contents = {
            "Q4 Financial Report. Revenue: $4.2M. Net profit: $3.1M.",
            "# Documentation\nUpdated with new sections and examples.",
            "server:\n  port: 8080\n  debug: true\n  timeout: 30s",
            "id,name,value\n1,alpha,100\n2,beta,200\n3,gamma,350",
            "public class App { public static void main(String[] a) {} }",
            "<html><body><h1>Home</h1><p>Welcome.</p></body></html>",
            "Meeting 15 Jan:\n- Q3 review\n- New roadmap items\n- Hiring plan",
            "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255));"
        };
        int ri = (int)(Math.random() * names.length);
        int rootId = getRootId();
        SimInode parent = inodes.stream().filter(i->i.isDir&&!i.deleted).findFirst().orElse(null);
        int pid = parent != null ? parent.id : rootId;
        createSimFile(pid, names[ri], contents[ri]);
        refreshAll();
    }

    void doCreateDir() {
        String[] dirs = {"documents","images","logs","source","backups","temp","config","archive"};
        int rootId = getRootId();
        String name = dirs[(int)(Math.random()*dirs.length)] + "_" + (int)(Math.random()*100);
        createSimDir(rootId, name);
        refreshAll();
    }

    void doDeleteRandom() {
        List<SimInode> files = new ArrayList<>();
        for (SimInode i : inodes) if (!i.isDir && !i.deleted) files.add(i);
        if (files.isEmpty()) { appendLog("WARN", "No files to delete."); return; }
        SimInode target = files.get((int)(Math.random()*files.size()));
        deleteInode(target.id);
        refreshAll();
    }

    void doReadFile() {
        List<SimInode> files = new ArrayList<>();
        for (SimInode i : inodes) if (!i.isDir && !i.deleted) files.add(i);
        if (files.isEmpty()) { appendLog("WARN", "No files to read."); return; }
        SimInode target = files.get((int)(Math.random()*files.size()));
        readOps++;
        appendLog("INFO", "Read: " + target.name + " (" + target.size + "B, inode " + target.id + ")");
        refreshAll();
    }

    void doBackup() {
        backup.clear();
        for (SimInode i : inodes) {
            if (!i.isDir && !i.deleted) {
                backup.put(i.id, new String[]{i.name, String.valueOf(i.parentId), String.valueOf(i.size)});
            }
        }
        appendLog("INFO", "Backup taken: " + backup.size() + " files snapshotted.");
        appendRecoveryLog("[BACKUP] " + backup.size() + " files saved.");
        refreshAll();
    }

    void doCrash() {
        int corrupted = 0;
        for (SimBlock b : disk) {
            if (b.state == BlockState.USED && Math.random() < 0.30) {
                b.state = BlockState.CORRUPTED;
                corrupted++;
            }
        }
        fragmentation = Math.min(100, fragmentation + corrupted * 4);
        appendLog("ERROR", "DISK CRASH: " + corrupted + " blocks corrupted.");
        appendRecoveryLog("[CRASH] " + corrupted + " blocks corrupted.");

        // mark affected inodes
        for (SimInode inode : inodes) {
            if (!inode.isDir && !inode.deleted) {
                for (int bid : inode.blocks) {
                    if (disk[bid].state == BlockState.CORRUPTED) {
                        inode.deleted = true;
                        break;
                    }
                }
            }
        }
        refreshAll();
        animateCrash();
    }

    void doRecover() {
        if (backup.isEmpty()) { appendLog("WARN", "No backup. Take backup first."); return; }
        int restored = 0, failed = 0;
        for (Map.Entry<Integer,String[]> e : backup.entrySet()) {
            int id = e.getKey();
            String[] snap = e.getValue();
            SimInode found = inodes.stream().filter(i->i.id==id).findFirst().orElse(null);
            if (found != null && found.deleted) {
                found.deleted = false;
                found.recovered = true;
                // mark recovered blocks green
                for (int bid : found.blocks) {
                    if (disk[bid].state == BlockState.CORRUPTED || disk[bid].state == BlockState.FREE) {
                        disk[bid].state = BlockState.RECOVERED;
                    }
                }
                recoveredCount++;
                restored++;
                appendRecoveryLog("[RESTORED] " + snap[0]);
            }
        }
        // also try to recover corrupted blocks
        for (SimBlock b : disk) {
            if (b.state == BlockState.CORRUPTED && Math.random() < 0.85) {
                b.state = BlockState.RECOVERED;
            }
        }
        appendLog("INFO", "Recovery: restored=" + restored + " failed=" + failed);
        appendRecoveryLog("[RECOVERY] Restored=" + restored + " | Rate=" + Math.round(restored*100.0/Math.max(1,restored+failed)) + "%");
        refreshAll();
    }

    void doScheduler() {
        int[] req = {98,183,37,122,14,124,65,67};
        int head = 53;

        // FCFS
        int fcfs = 0, cur = head;
        for (int r : req) { fcfs += Math.abs(r - cur); cur = r; }

        // SSTF
        List<Integer> rem = new ArrayList<>();
        for (int r : req) rem.add(r);
        int sstf = 0; cur = head;
        while (!rem.isEmpty()) {
            final int c = cur;
            int closest = rem.stream().min(Comparator.comparingInt(r->Math.abs(r-c))).get();
            sstf += Math.abs(cur - closest); cur = closest; rem.remove((Integer)closest);
        }

        // SCAN
        List<Integer> sorted = new ArrayList<>();
        for (int r : req) sorted.add(r);
        Collections.sort(sorted);
        int scan = 0; cur = head;
        List<Integer> right = new ArrayList<>(), left = new ArrayList<>();
        for (int r : sorted) { if (r>=cur) right.add(r); else left.add(r); }
        Collections.reverse(left);
        for (int r : right) { scan += Math.abs(cur-r); cur=r; }
        if (cur < 199) { scan += (199-cur); cur=199; }
        for (int r : left) { scan += Math.abs(cur-r); cur=r; }

        String best = sstf<=scan&&sstf<=fcfs?"SSTF":scan<=fcfs?"SCAN":"FCFS";
        int bestVal = Math.min(Math.min(fcfs,sstf),scan);

        StringBuilder sb = new StringBuilder();
        sb.append("Requests : [98, 183, 37, 122, 14, 124, 65, 67]\n");
        sb.append("Head pos : 53\n\n");
        sb.append(String.format("%-6s : %d track movements%n", "FCFS",  fcfs));
        sb.append(String.format("%-6s : %d track movements%n", "SSTF",  sstf));
        sb.append(String.format("%-6s : %d track movements%n", "SCAN",  scan));
        sb.append("\n★ Best: " + best + " (" + bestVal + " movements)\n");
        sb.append("  SSTF reduces movement by " + Math.round((fcfs-sstf)*100.0/fcfs) + "% vs FCFS");

        schedArea.setText(sb.toString());
        appendLog("INFO", "Scheduler: FCFS=" + fcfs + " SSTF=" + sstf + " SCAN=" + scan + " → Best=" + best);
    }

    void doDefrag() {
        int before = fragmentation;
        fragmentation = Math.max(0, (int)(fragmentation * 0.08));
        int freed = 0;
        // compact recovered blocks
        for (SimBlock b : disk) {
            if (b.state == BlockState.RECOVERED) { b.state = BlockState.USED; freed++; }
        }
        String report =
            "Before : " + before + "% fragmented\n" +
            "After  : " + fragmentation + "% fragmented\n" +
            "Gain   : -" + (before-fragmentation) + " percentage points\n" +
            "Blocks compacted: " + freed;
        JOptionPane.showMessageDialog(this, report, "Defrag Complete", JOptionPane.INFORMATION_MESSAGE);
        appendLog("INFO", "Defrag: " + before + "% → " + fragmentation + "%");
        refreshAll();
    }

    void doFullDemo() {
        appendLog("INFO", "=== FULL SIMULATION START ===");
        Timer t = new Timer();
        t.schedule(task(()-> {
            doCreateFile(); doCreateFile(); doCreateDir();
        }), 0);
        t.schedule(task(()-> { doCreateFile(); doReadFile(); }), 400);
        t.schedule(task(this::doBackup), 800);
        t.schedule(task(this::doCrash), 1300);
        t.schedule(task(this::doDeleteRandom), 1700);
        t.schedule(task(this::doRecover), 2100);
        t.schedule(task(this::doScheduler), 2500);
        t.schedule(task(()-> {
            doDefrag();
            appendLog("INFO", "=== SIMULATION COMPLETE ===");
        }), 2900);
    }

    // ── FS helpers ────────────────────────────────────────────────────────────
    int getRootId() {
        return inodes.stream().filter(i->i.isDir&&i.parentId==-1).mapToInt(i->i.id).findFirst().orElse(0);
    }

    void createSimDir(int parentId, String name) {
        SimInode n = new SimInode(nextId++, name, true, parentId, 0);
        inodes.add(n);
        journal.add("CREATE_DIR | id=" + n.id + " | name=" + name);
        appendLog("INFO", "Created dir: " + name + " (inode " + n.id + ")");
    }

    void createSimFile(int parentId, String name, String content) {
        int blocksNeeded = Math.max(1, (int)Math.ceil((double)content.length()/BLOCK_SIZE));
        List<Integer> blockIds = new ArrayList<>();
        for (int i = 0; i < blocksNeeded; i++) {
            SimBlock b = findFreeBlock();
            if (b == null) { appendLog("ERROR", "Disk full!"); return; }
            b.state = BlockState.USED;
            b.data  = content.substring(Math.min(i*BLOCK_SIZE, content.length()),
                                        Math.min((i+1)*BLOCK_SIZE, content.length()));
            blockIds.add(b.id);
        }
        SimInode n = new SimInode(nextId++, name, false, parentId, content.length());
        n.blocks = blockIds;
        inodes.add(n);
        writeOps++;
        journal.add("CREATE_FILE | id=" + n.id + " | name=" + name + " | size=" + content.length() + " | blocks=" + blockIds);
        appendLog("INFO", "Created: " + name + " (" + content.length() + "B, inode " + n.id + ")");
    }

    void deleteInode(int id) {
        for (SimInode i : inodes) {
            if (i.id == id && !i.deleted) {
                i.blocks.forEach(bid -> disk[bid].state = BlockState.FREE);
                i.deleted = true;
                journal.add("DELETE | id=" + id + " | name=" + i.name);
                appendLog("WARN", "Deleted: " + i.name + " (inode " + id + ")");
                break;
            }
        }
    }

    SimBlock findFreeBlock() {
        for (SimBlock b : disk) if (b.state == BlockState.FREE) return b;
        return null;
    }

    // ── log helpers ───────────────────────────────────────────────────────────
    void appendLog(String level, String msg) {
        String ts = String.format("%tT", new Date());
        String line = "[" + ts + "] [" + level + "] " + msg + "\n";
        sysLog.add(line);
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    void appendRecoveryLog(String msg) {
        String ts = String.format("%tT", new Date());
        String line = "[" + ts + "] " + msg + "\n";
        recoveryLog.add(line);
        SwingUtilities.invokeLater(() -> {
            recLogArea.append(line);
            recLogArea.setCaretPosition(recLogArea.getDocument().getLength());
        });
    }

    // ── refresh ───────────────────────────────────────────────────────────────
    void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            updateMetrics();
            updateTree();
            updateJournal();
            diskMap.repaint();
        });
    }

    void updateMetrics() {
        long used     = 0, corrupt = 0, recovered = 0;
        for (SimBlock b : disk) {
            if (b.state == BlockState.USED)      used++;
            if (b.state == BlockState.CORRUPTED) corrupt++;
            if (b.state == BlockState.RECOVERED) recovered++;
        }
        long free = TOTAL_BLOCKS - used - corrupt - recovered;
        mcUsed.setValue(String.valueOf(used));
        mcFree.setValue(String.valueOf(free));
        mcCorrupt.setValue(String.valueOf(corrupt));
        mcRecovery.setValue(String.valueOf(recoveredCount));
        usageBar.setValue((int)(used*100/TOTAL_BLOCKS));
        fragBar.setValue(fragmentation);

        // bottom bar stats
        for (Component c : getContentPane().getComponents()) updateStatLabels(c);
    }

    void updateStatLabels(Component c) {
        if (c instanceof JPanel p) {
            for (Component ch : p.getComponents()) {
                if (ch instanceof JLabel l) {
                    if ("readOpsLabel".equals(l.getName()))  l.setText(String.valueOf(readOps));
                    if ("writeOpsLabel".equals(l.getName())) l.setText(String.valueOf(writeOps));
                }
                updateStatLabels(ch);
            }
        }
    }

    void updateTree() {
        treeRoot.removeAllChildren();
        buildTreeNode(treeRoot, -1);
        treeModel.reload();
        for (int i = 0; i < fileTree.getRowCount(); i++) fileTree.expandRow(i);
    }

    void buildTreeNode(DefaultMutableTreeNode parent, int parentId) {
        for (SimInode inode : inodes) {
            if (inode.parentId != parentId) continue;
            String label = inode.isDir
                ? "📁 " + inode.name + "/"
                : (inode.deleted   ? "✕ " + inode.name
                 : inode.recovered ? "↩ [R] " + inode.name + " [" + inode.size + "B]"
                 :                   "📄 " + inode.name + " [" + inode.size + "B]");
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
            parent.add(node);
            if (inode.isDir) buildTreeNode(node, inode.id);
        }
    }

    void updateJournal() {
        StringBuilder sb = new StringBuilder();
        for (String j : journal) sb.append(j).append("\n");
        journalArea.setText(sb.toString());
        journalArea.setCaretPosition(journalArea.getDocument().getLength());
    }

    void animateCrash() {
        Timer t = new Timer();
        for (int i = 0; i < 6; i++) {
            final int fi = i;
            t.schedule(task(() -> {
                diskMap.setFlash(fi % 2 == 0);
                diskMap.repaint();
            }), i * 120L);
        }
        t.schedule(task(() -> { diskMap.setFlash(false); diskMap.repaint(); }), 750);
    }

    void seedInitialFS() {
        SimInode root = new SimInode(nextId++, "/", true, -1, 0);
        inodes.add(root);
        int rootId = root.id;

        int docs = nextId;
        createSimDir(rootId, "documents");
        int imgs = nextId;
        createSimDir(rootId, "images");
        int logs = nextId;
        createSimDir(rootId, "logs");

        createSimFile(docs, "report.txt",   "Q4 Financial Report. Revenue: $4.2M. Net: $3.1M.");
        createSimFile(docs, "readme.md",    "# FSROT\nFile System Recovery & Optimization Tool.");
        createSimFile(imgs, "logo.png",     "PNG:89504E470D0A...[binary simulation data]");
        createSimFile(logs, "system.log",   "2024-01-15 [INFO] Booted.\n2024-01-15 [ERROR] Disk spike.");
        createSimFile(docs, "Main.java",    "public class Main { public static void main(String[] a) { } }");
    }

    // ── UI factories ──────────────────────────────────────────────────────────
    JPanel panel(Color bg, LayoutManager lm) {
        JPanel p = new JPanel(lm); p.setBackground(bg); return p;
    }

    TitledBorder cardBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER, 1), "  " + title + "  ");
        b.setTitleFont(TITLE_F);
        b.setTitleColor(TEXT_DIM);
        return b;
    }

    JScrollPane darkScroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBackground(SURFACE);
        s.getViewport().setBackground(SURFACE);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getVerticalScrollBar().setBackground(SURFACE);
        s.getHorizontalScrollBar().setBackground(SURFACE);
        return s;
    }

    JTextArea darkTextArea() {
        JTextArea a = new JTextArea();
        a.setBackground(SURFACE);
        a.setForeground(TEXT);
        a.setFont(MONO);
        a.setEditable(false);
        a.setCaretColor(ACCENT);
        a.setBorder(new EmptyBorder(4,6,4,6));
        return a;
    }

    JButton actionBtn(String text, Color color, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(MONO_B);
        b.setForeground(color);
        b.setBackground(SURFACE2);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(color.getRed(),color.getGreen(),color.getBlue(),40)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(SURFACE2); }
        });
        b.addActionListener(e -> action.run());
        return b;
    }

    JButton smallBtn(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.PLAIN, 10));
        b.setForeground(TEXT_DIM);
        b.setBackground(SURFACE);
        b.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
        b.setFocusPainted(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    JLabel makePill(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 10));
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 1),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return l;
    }

    JLabel dimLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(MONO); l.setForeground(TEXT_DIM); return l;
    }

    JLabel statLabel(String text, String name) {
        JLabel l = new JLabel(text);
        l.setName(name); l.setFont(MONO_B); l.setForeground(ACCENT); return l;
    }

    JProgressBar makeBar(Color color, int val) {
        JProgressBar b = new JProgressBar(0, 100);
        b.setValue(val);
        b.setForeground(color);
        b.setBackground(SURFACE2);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(0, 6));
        return b;
    }

    JPanel barRow(String label, JProgressBar bar) {
        JPanel r = new JPanel(new BorderLayout(6,0));
        r.setOpaque(false);
        JLabel l = dimLabel(label);
        l.setPreferredSize(new Dimension(36,0));
        r.add(l,   BorderLayout.WEST);
        r.add(bar, BorderLayout.CENTER);
        return r;
    }

    JPanel buildDiskLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.setBackground(SURFACE);
        p.add(legendItem("FREE",      BG));
        p.add(legendItem("USED",      ACCENT));
        p.add(legendItem("CORRUPTED", RED));
        p.add(legendItem("RECOVERED", GREEN));
        return p;
    }

    JPanel legendItem(String label, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRoundRect(0, 0, 10, 10, 3, 3);
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 10));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Monospaced", Font.PLAIN, 10));
        l.setForeground(TEXT_DIM);
        p.add(dot); p.add(l);
        return p;
    }

    void styleTree(JTree tree) {
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree t, Object v, boolean sel,
                    boolean exp, boolean leaf, int row, boolean focus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(t,v,sel,exp,leaf,row,focus);
                l.setBackground(sel ? SURFACE2 : SURFACE);
                l.setForeground(TEXT);
                l.setFont(MONO);
                String txt = v.toString();
                if (txt.contains("✕"))  l.setForeground(RED);
                if (txt.contains("↩"))  l.setForeground(GREEN);
                if (txt.contains("📁")) l.setForeground(ACCENT);
                setBackgroundNonSelectionColor(SURFACE);
                setBackgroundSelectionColor(SURFACE2);
                setBorderSelectionColor(BORDER);
                return l;
            }
        });
    }

    TimerTask task(Runnable r) {
        return new TimerTask() { public void run() { SwingUtilities.invokeLater(r); } };
    }

    // ── disk map panel ────────────────────────────────────────────────────────
    class DiskMapPanel extends JPanel {
        boolean flash = false;
        DiskMapPanel() { setBackground(SURFACE); setPreferredSize(new Dimension(0, 120)); }
        void setFlash(boolean f) { flash = f; }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int cols = 32, rows = TOTAL_BLOCKS / cols;
            int pad = 10;
            int cellW = (W - pad*2) / cols;
            int cellH = (H - pad*2) / rows;
            int bw = cellW - 2, bh = cellH - 2;

            if (flash) {
                g.setColor(new Color(248,113,113,40));
                g.fillRect(0,0,W,H);
            }

            for (SimBlock b : disk) {
                int col = b.id % cols, row = b.id / cols;
                int x = pad + col * cellW + 1;
                int y = pad + row * cellH + 1;
                Color c = switch (b.state) {
                    case FREE      -> SURFACE2;
                    case USED      -> ACCENT;
                    case CORRUPTED -> RED;
                    case RECOVERED -> GREEN;
                };
                g.setColor(c);
                g.fillRoundRect(x, y, bw, bh, 2, 2);
            }
        }
    }

    // ── metric card component ─────────────────────────────────────────────────
    class MetricCard extends JPanel {
        JLabel valueLabel;
        MetricCard(String title, String value, Color color) {
            setLayout(new BorderLayout(0,4));
            setBackground(SURFACE2);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER,1),
                BorderFactory.createEmptyBorder(8,10,8,10)));
            JLabel tl = new JLabel(title);
            tl.setFont(new Font("Monospaced",Font.BOLD,9));
            tl.setForeground(TEXT_DIM);
            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font("Monospaced",Font.BOLD,22));
            valueLabel.setForeground(color);
            add(tl,         BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
        }
        void setValue(String v) { valueLabel.setText(v); }
    }

    // ── main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        // global dark defaults
        UIManager.put("Panel.background",          BG);
        UIManager.put("ScrollPane.background",     SURFACE);
        UIManager.put("Viewport.background",       SURFACE);
        UIManager.put("TextArea.background",       SURFACE);
        UIManager.put("TextArea.foreground",       TEXT);
        UIManager.put("Tree.background",           SURFACE);
        UIManager.put("Tree.foreground",           TEXT);
        UIManager.put("ProgressBar.background",    SURFACE2);
        UIManager.put("ProgressBar.foreground",    ACCENT);
        UIManager.put("ScrollBar.background",      SURFACE);
        UIManager.put("ScrollBar.thumb",           BORDER);

        SwingUtilities.invokeLater(FSROT_GUI::new);
    }
}
