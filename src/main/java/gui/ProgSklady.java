package gui;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sklady.GeneratorProduktu;
import sklady.Produkt;
import sklady.Sklady;
import sklady.enumPozice;
import sklady.enumSklad;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;


public class ProgSklady extends Application {


    private record Radek(enumSklad sklad, Produkt produkt, boolean aktualni) { }

    private static final PseudoClass AKTUALNI = PseudoClass.getPseudoClass("aktualni");
    private static final DateTimeFormatter CAS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Sklady sklady = new Sklady();
    private final GeneratorProduktu generator = new GeneratorProduktu();

    private Stage okno;


    private final ToggleGroup skupinaSkladu = new ToggleGroup();
    private final ToggleButton tlVsechny = new ToggleButton("Všechny sklady");
    private final ToggleButton[] tlSklady = new ToggleButton[enumSklad.values().length];
    private final SimpleBooleanProperty vybranyVsechny = new SimpleBooleanProperty(true);

    // tabulka a filtr
    private final ObservableList<Radek> radky = FXCollections.observableArrayList();
    private final TableView<Radek> tabulka = new TableView<>(radky);
    private final Label lblNadpis = new Label();
    private final TextField tfLimit = new TextField("10");
    private Integer aktivniLimit = null;          // null = filtr vypnutý

    // formulář produktu
    private final TextField tfId = new TextField();
    private final TextField tfNazev = new TextField();
    private final TextField tfKategorie = new TextField();
    private final TextField tfCena = new TextField();
    private final TextField tfMnozstvi = new TextField();
    private final ToggleGroup skupinaPozice = new ToggleGroup();

    // přesun
    private final ComboBox<enumSklad> cbZdroj = new ComboBox<>();
    private final ComboBox<enumSklad> cbCil = new ComboBox<>();

    // generátor
    private final TextField tfPocet = new TextField("10");
    private final RadioButton rbDoVybraneho = new RadioButton("do vybraného skladu");
    private final RadioButton rbNahodne = new RadioButton("náhodně do všech skladů");

    // statistiky, log, stavový řádek
    private final Label lblPrumerVelky = new Label("–");
    private final TextArea taLog = new TextArea();
    private final Label lblPocet = new Label();
    private final Label lblPrumer = new Label();

    @Override
    public void start(Stage stage) {
        okno = stage;

        BorderPane koren = new BorderPane();
        koren.setTop(new VBox(vytvorMenu(), vytvorListuSkladu()));

        SplitPane stred = new SplitPane(vytvorStred(), vytvorPravyPanel());
        stred.setDividerPositions(0.66);
        SplitPane.setResizableWithParent(stred.getItems().get(1), false);
        koren.setCenter(stred);
        koren.setBottom(vytvorStavovyRadek());

        Scene scena = new Scene(koren, 1240, 780);
        URL styl = ProgSklady.class.getResource("styl.css");
        if (styl != null) {
            scena.getStylesheets().add(styl.toExternalForm());
        }

        stage.setTitle("ProgSklady – Datové struktury, semestrální práce A");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scena);
        stage.show();

        log("Aplikace spuštěna. Data lze zadat formulářem (záložka Produkt), importovat ze souboru "
                + "(Soubor → Importovat CSV) nebo vygenerovat (záložka Generátor).");
        obnov();
    }

    // ================================================================ sestavení okna

    private MenuBar vytvorMenu() {
        MenuItem miImport = new MenuItem("Importovat CSV…");
        miImport.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        miImport.setOnAction(e -> importuj());

        MenuItem miVypis = new MenuItem("Uložit výpis tabulky…");
        miVypis.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        miVypis.setOnAction(e -> ulozVypis());

        MenuItem miLog = new MenuItem("Uložit log…");
        miLog.setOnAction(e -> ulozLog());

        MenuItem miKonec = new MenuItem("Konec");
        miKonec.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        miKonec.setOnAction(e -> okno.close());

        Menu soubor = new Menu("Soubor", null, miImport, miVypis, miLog, new SeparatorMenuItem(), miKonec);

        MenuItem miZrusVybrany = new MenuItem("Zrušit vybraný sklad");
        miZrusVybrany.setOnAction(e -> zrus(false));
        MenuItem miZrusVse = new MenuItem("Zrušit všechny sklady");
        miZrusVse.setOnAction(e -> zrus(true));
        Menu sklad = new Menu("Sklad", null, miZrusVybrany, miZrusVse);

        MenuItem miOAplikaci = new MenuItem("O aplikaci");
        miOAplikaci.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Sklady jsou pole abstraktních obousměrných necyklických seznamů.\n\n"
                            + "Žlutý řádek v tabulce je aktuální prvek seznamu daného skladu. "
                            + "Mění se tlačítky v části \"Pohyb v seznamu\".\n\n"
                            + "Předchůdce a následník se vždy počítají vůči aktuálnímu prvku.",
                    ButtonType.OK);
            a.initOwner(okno);
            a.setHeaderText("ProgSklady");
            a.showAndWait();
        });
        Menu napoveda = new Menu("Nápověda", null, miOAplikaci);

        return new MenuBar(soubor, sklad, napoveda);
    }

    // tmavá lišta nahoře: název aplikace + přepínač skladů
    private HBox vytvorListuSkladu() {
        Label nazev = new Label("ProgSklady");
        nazev.getStyleClass().add("nazev-aplikace");

        tlVsechny.setToggleGroup(skupinaSkladu);
        tlVsechny.getStyleClass().add("sklad-pill");
        tlVsechny.setSelected(true);

        HBox lista = new HBox(8, nazev, new Separator(Orientation.VERTICAL), tlVsechny);
        for (enumSklad s : enumSklad.values()) {
            ToggleButton tl = new ToggleButton(s.getNazev());
            tl.setUserData(s);
            tl.setToggleGroup(skupinaSkladu);
            tl.getStyleClass().add("sklad-pill");
            tlSklady[s.getIndex()] = tl;
            lista.getChildren().add(tl);
        }

        // kliknutí na už vybraný sklad by výběr zrušil - to nechceme, vždy je něco vybráno
        skupinaSkladu.selectedToggleProperty().addListener((obs, stary, novy) -> {
            if (novy == null) {
                stary.setSelected(true);
                return;
            }
            vybranyVsechny.set(novy == tlVsechny);
            enumSklad s = vybranySklad();
            if (s != null) {
                cbZdroj.setValue(s);
            }
            obnov();
        });

        Region mezera = new Region();
        HBox.setHgrow(mezera, Priority.ALWAYS);
        Button btImport = new Button("Importovat CSV");
        btImport.getStyleClass().add("lista-tlacitko");
        btImport.setOnAction(e -> importuj());
        lista.getChildren().addAll(mezera, btImport);

        lista.setAlignment(Pos.CENTER_LEFT);
        lista.setPadding(new Insets(10, 14, 10, 14));
        lista.getStyleClass().add("horni-lista");
        return lista;
    }

    // tabulka s filtrem nahoře a logem dole
    private VBox vytvorStred() {
        lblNadpis.getStyleClass().add("nadpis-tabulky");

        tfLimit.setPrefColumnCount(4);
        tfLimit.setOnAction(e -> zapniFiltr());
        Button btFiltr = new Button("Pod limitem");
        btFiltr.setOnAction(e -> zapniFiltr());
        Button btVse = new Button("Zobrazit vše");
        btVse.setOnAction(e -> {
            aktivniLimit = null;
            log("Filtr zrušen, zobrazeny všechny produkty.");
            obnov();
        });

        Region mezera = new Region();
        HBox.setHgrow(mezera, Priority.ALWAYS);
        HBox hlavicka = new HBox(8, lblNadpis, mezera, new Label("Množství menší než"), tfLimit, btFiltr, btVse);
        hlavicka.setAlignment(Pos.CENTER_LEFT);

        vytvorTabulku();
        VBox.setVgrow(tabulka, Priority.ALWAYS);

        taLog.setEditable(false);
        taLog.setWrapText(true);
        taLog.setPrefRowCount(7);
        taLog.getStyleClass().add("log");
        TitledPane logPanel = new TitledPane("Log operací", taLog);
        logPanel.setCollapsible(true);

        VBox stred = new VBox(8, hlavicka, tabulka, logPanel);
        stred.setPadding(new Insets(10));
        return stred;
    }

    private void vytvorTabulku() {
        TableColumn<Radek, String> sSklad = sloupec("Sklad", r -> r.sklad().getNazev(), 80);
        TableColumn<Radek, String> sId = sloupec("ID", r -> r.produkt().getId(), 80);
        TableColumn<Radek, String> sNazev = sloupec("Název", r -> r.produkt().getNazev(), 230);
        TableColumn<Radek, String> sKategorie = sloupec("Kategorie", r -> r.produkt().getKategorie(), 160);
        TableColumn<Radek, Double> sCena = sloupec("Cena za kus [Kč]", r -> r.produkt().getCena(), 120);
        TableColumn<Radek, Integer> sMnozstvi = sloupec("Množství [ks]", r -> r.produkt().getMnozstvi(), 100);
        sCena.getStyleClass().add("cislo");
        sMnozstvi.getStyleClass().add("cislo");
        sCena.setCellFactory(sl -> new TableCell<>() {
            @Override
            protected void updateItem(Double cena, boolean prazdna) {
                super.updateItem(cena, prazdna);
                setText(prazdna || cena == null ? null : String.format(Locale.ROOT, "%.2f", cena));
            }
        });

        tabulka.getColumns().add(sSklad);
        tabulka.getColumns().add(sId);
        tabulka.getColumns().add(sNazev);
        tabulka.getColumns().add(sKategorie);
        tabulka.getColumns().add(sCena);
        tabulka.getColumns().add(sMnozstvi);

        tabulka.setPlaceholder(new Label("Žádné produkty. Importujte CSV, vygenerujte produkty nebo vložte produkt."));
        tabulka.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // aktuální prvek seznamu se zvýrazní (barvu určuje styl.css přes pseudotřídu :aktualni)
        tabulka.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Radek radek, boolean prazdny) {
                super.updateItem(radek, prazdny);
                pseudoClassStateChanged(AKTUALNI, !prazdny && radek != null && radek.aktualni());
            }
        });
    }

    private <T> TableColumn<Radek, T> sloupec(String nazev, Function<Radek, T> hodnota, double sirka) {
        TableColumn<Radek, T> s = new TableColumn<>(nazev);
        s.setCellValueFactory(bunka -> new ReadOnlyObjectWrapper<>(hodnota.apply(bunka.getValue())));
        s.setPrefWidth(sirka);
        s.setSortable(false);   // pořadí řádků = pořadí v seznamu, nesmí se přeházet
        return s;
    }

    private TabPane vytvorPravyPanel() {
        Tab produkt = new Tab("Produkt", posouvatelne(zalozkaProdukt()));
        Tab presun = new Tab("Přesun", posouvatelne(zalozkaPresun()));
        Tab gen = new Tab("Generátor", posouvatelne(zalozkaGenerator()));
        Tab sklad = new Tab("Sklad", posouvatelne(zalozkaSklad()));
        TabPane panel = new TabPane(produkt, presun, gen, sklad);
        panel.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        panel.setMinWidth(360);
        return panel;
    }

    private ScrollPane posouvatelne(VBox obsah) {
        obsah.setPadding(new Insets(14));
        ScrollPane sp = new ScrollPane(obsah);
        sp.setFitToWidth(true);
        return sp;
    }

    private VBox zalozkaProdukt() {
        Label upozorneni = new Label("Vyberte nahoře konkrétní sklad – operace se seznamem nejdou nad všemi sklady najednou.");
        upozorneni.setWrapText(true);
        upozorneni.getStyleClass().add("upozorneni");
        upozorneni.visibleProperty().bind(vybranyVsechny);
        upozorneni.managedProperty().bind(vybranyVsechny);

        // --- pohyb v seznamu = operace zpřístupni
        Button btPrvni = navigace("« První", enumPozice.PRVNI);
        Button btAktualni = navigace("Aktuální", enumPozice.AKTUALNI);
        Button btPosledni = navigace("Poslední »", enumPozice.POSLEDNI);
        Button btPredchudce = navigace("‹ Předchůdce", enumPozice.PREDCHUDCE);
        Button btNaslednik = navigace("Následník ›", enumPozice.NASLEDNIK);

        GridPane pohyb = new GridPane();
        pohyb.setHgap(6);
        pohyb.setVgap(6);
        pohyb.add(btPrvni, 0, 0);
        pohyb.add(btAktualni, 1, 0);
        pohyb.add(btPosledni, 2, 0);
        pohyb.add(btPredchudce, 0, 1);
        pohyb.add(btNaslednik, 2, 1);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            pohyb.getColumnConstraints().add(c);
        }

        // --- formulář (vstup z klávesnice)
        GridPane formular = new GridPane();
        formular.setHgap(8);
        formular.setVgap(6);
        pridejPole(formular, 0, "ID:", tfId, "např. P061");
        pridejPole(formular, 1, "Název:", tfNazev, "např. Webkamera Full HD");
        pridejPole(formular, 2, "Kategorie:", tfKategorie, "např. Elektronika");
        pridejPole(formular, 3, "Cena za kus:", tfCena, "např. 1190,00");
        pridejPole(formular, 4, "Množství:", tfMnozstvi, "např. 25");
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        formular.getColumnConstraints().addAll(c0, c1);

        // --- pozice pro vložení / odebrání
        FlowPane pozice = new FlowPane(6, 6);
        for (enumPozice p : enumPozice.values()) {
            ToggleButton tl = new ToggleButton(nazevPozice(p));
            tl.setUserData(p);
            tl.setToggleGroup(skupinaPozice);
            tl.getStyleClass().add("pozice");
            if (p == enumPozice.POSLEDNI) {
                tl.setSelected(true);
            }
            pozice.getChildren().add(tl);
        }
        skupinaPozice.selectedToggleProperty().addListener((obs, stary, novy) -> {
            if (novy == null) {
                stary.setSelected(true);
            }
        });

        Button btVlozit = new Button("Vložit produkt");
        btVlozit.getStyleClass().add("primarni");
        btVlozit.setMaxWidth(Double.MAX_VALUE);
        btVlozit.setOnAction(e -> vloz());
        Button btOdebrat = new Button("Odebrat z pozice");
        btOdebrat.getStyleClass().add("nebezpecne");
        btOdebrat.setMaxWidth(Double.MAX_VALUE);
        btOdebrat.setOnAction(e -> odeber());
        HBox.setHgrow(btVlozit, Priority.ALWAYS);
        HBox.setHgrow(btOdebrat, Priority.ALWAYS);
        HBox akce = new HBox(8, btVlozit, btOdebrat);

        for (Button b : List.of(btPrvni, btAktualni, btPosledni, btPredchudce, btNaslednik, btVlozit, btOdebrat)) {
            b.disableProperty().bind(vybranyVsechny);
        }

        // Enter v kterémkoli poli formuláře = vložit
        for (TextField tf : List.of(tfId, tfNazev, tfKategorie, tfCena, tfMnozstvi)) {
            tf.setOnAction(e -> vloz());
        }

        Label napoveda = new Label("Předchůdce a následník se počítají vůči aktuálnímu (žlutému) prvku. "
                + "Pozici \"aktuální\" lze použít jen pro odebrání.");
        napoveda.setWrapText(true);
        napoveda.getStyleClass().add("napoveda");

        return new VBox(10,
                upozorneni,
                sekce("Pohyb v seznamu (zpřístupnit)"), pohyb,
                new Separator(),
                sekce("Produkt z klávesnice"), formular,
                sekce("Pozice"), pozice, akce, napoveda);
    }

    private VBox zalozkaPresun() {
        nastavVyberSkladu(cbZdroj);
        nastavVyberSkladu(cbCil);
        cbZdroj.setValue(enumSklad.SKLAD_1);
        cbCil.setValue(enumSklad.SKLAD_2);

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("Ze skladu:"), cbZdroj);
        g.addRow(1, new Label("Do skladu:"), cbCil);

        Button bt = new Button("Přesunout aktuální produkt");
        bt.getStyleClass().add("primarni");
        bt.setMaxWidth(Double.MAX_VALUE);
        bt.setOnAction(e -> presun());

        Label napoveda = new Label("Přesune se aktuální produkt zdrojového skladu. Ve zdroji se pak aktuálním "
                + "stane první produkt, v cíli se produkt zařadí na konec.");
        napoveda.setWrapText(true);
        napoveda.getStyleClass().add("napoveda");

        return new VBox(10, sekce("Přesun produktu mezi sklady"), g, bt, napoveda);
    }

    private VBox zalozkaGenerator() {
        ToggleGroup kam = new ToggleGroup();
        rbDoVybraneho.setToggleGroup(kam);
        rbNahodne.setToggleGroup(kam);
        rbNahodne.setSelected(true);

        tfPocet.setPrefColumnCount(5);
        tfPocet.setOnAction(e -> generuj());
        HBox pocet = new HBox(8, new Label("Počet produktů:"), tfPocet);
        pocet.setAlignment(Pos.CENTER_LEFT);

        Button bt = new Button("Generovat produkty Apple");
        bt.getStyleClass().add("primarni");
        bt.setMaxWidth(Double.MAX_VALUE);
        bt.setOnAction(e -> generuj());

        Label napoveda = new Label("Generátor vytváří náhodné produkty Apple (iPhone, iPad, Mac, Apple Watch, "
                + "audio, domácnost, příslušenství, monitory) s ID G0001, G0002, … "
                + "a vkládá je na konec seznamu.");
        napoveda.setWrapText(true);
        napoveda.getStyleClass().add("napoveda");

        return new VBox(10, sekce("Generátor produktů Apple"), pocet, rbDoVybraneho, rbNahodne, bt, napoveda);
    }

    private VBox zalozkaSklad() {
        lblPrumerVelky.getStyleClass().add("velke-cislo");
        Button btPrumer = new Button("Zjistit průměrnou cenu");
        btPrumer.setMaxWidth(Double.MAX_VALUE);
        btPrumer.setOnAction(e -> prumernaCena());

        Button btZrus = new Button("Zrušit vybraný sklad");
        btZrus.getStyleClass().add("nebezpecne");
        btZrus.setMaxWidth(Double.MAX_VALUE);
        btZrus.disableProperty().bind(vybranyVsechny);
        btZrus.setOnAction(e -> zrus(false));

        Button btZrusVse = new Button("Zrušit všechny sklady");
        btZrusVse.getStyleClass().add("nebezpecne");
        btZrusVse.setMaxWidth(Double.MAX_VALUE);
        btZrusVse.setOnAction(e -> zrus(true));

        Button btVypis = new Button("Uložit výpis tabulky do souboru…");
        btVypis.setMaxWidth(Double.MAX_VALUE);
        btVypis.setOnAction(e -> ulozVypis());
        Button btLog = new Button("Uložit log do souboru…");
        btLog.setMaxWidth(Double.MAX_VALUE);
        btLog.setOnAction(e -> ulozLog());

        return new VBox(10,
                sekce("Průměrná cena za kus"), lblPrumerVelky, btPrumer,
                new Separator(),
                sekce("Výstup do souboru"), btVypis, btLog,
                new Separator(),
                sekce("Zrušení"), btZrus, btZrusVse);
    }

    private HBox vytvorStavovyRadek() {
        Label legenda = new Label("Žlutě = aktuální prvek seznamu");
        legenda.getStyleClass().add("legenda");
        Region mezera = new Region();
        HBox.setHgrow(mezera, Priority.ALWAYS);
        HBox radek = new HBox(16, lblPocet, new Separator(Orientation.VERTICAL), lblPrumer, mezera, legenda);
        radek.setAlignment(Pos.CENTER_LEFT);
        radek.getStyleClass().add("stavovy-radek");
        return radek;
    }

    // ---------------------------------------------------------------- malé pomocníky pro sestavení okna

    private Label sekce(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("sekce");
        return l;
    }

    private Button navigace(String text, enumPozice pozice) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> zpristupni(pozice));
        return b;
    }

    private void pridejPole(GridPane g, int radek, String popis, TextField pole, String priklad) {
        pole.setPromptText(priklad);
        g.add(new Label(popis), 0, radek);
        g.add(pole, 1, radek);
    }

    private void nastavVyberSkladu(ComboBox<enumSklad> cb) {
        cb.getItems().setAll(enumSklad.values());
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(enumSklad s) {
                return s == null ? "" : s.getNazev();
            }

            @Override
            public enumSklad fromString(String text) {
                return null;   // ComboBox není editovatelný, nepoužívá se
            }
        });
        cb.setMaxWidth(Double.MAX_VALUE);
    }

    private static String nazevPozice(enumPozice p) {
        return switch (p) {
            case PRVNI -> "první";
            case POSLEDNI -> "poslední";
            case PREDCHUDCE -> "předchůdce";
            case NASLEDNIK -> "následník";
            case AKTUALNI -> "aktuální";
        };
    }

    // ================================================================ operace

    private void zpristupni(enumPozice pozice) {
        enumSklad s = vybranySklad();
        if (s == null) {
            return;
        }
        boolean ok = proved("Zpřístupnění", () -> {
            Produkt p = sklady.zpristupniProdukt(pozice, s);
            log(s.getNazev() + ": zpřístupněn " + nazevPozice(pozice) + " → " + p);
        });
        if (ok) {
            oznacAktualni(s);
        }
    }

    private void vloz() {
        enumSklad s = vybranySklad();
        if (s == null) {
            chyba("Vložení produktu", "Vyberte nahoře konkrétní sklad.");
            return;
        }
        enumPozice pozice = vybranaPozice();
        boolean ok = proved("Vložení produktu", () -> {
            Produkt p = produktZFormulare();
            sklady.vlozProdukt(p, pozice, s);
            log(s.getNazev() + ": vložen produkt na pozici " + nazevPozice(pozice) + " → " + p);
        });
        if (ok) {
            for (TextField tf : List.of(tfId, tfNazev, tfKategorie, tfCena, tfMnozstvi)) {
                tf.clear();
            }
            tfId.requestFocus();
        }
    }

    private void odeber() {
        enumSklad s = vybranySklad();
        if (s == null) {
            return;
        }
        enumPozice pozice = vybranaPozice();
        proved("Odebrání produktu", () -> {
            Produkt p = sklady.odeberProdukt(pozice, s);
            log(s.getNazev() + ": odebrán " + nazevPozice(pozice) + " → " + p);
        });
    }

    private void presun() {
        enumSklad zdroj = cbZdroj.getValue();
        enumSklad cil = cbCil.getValue();
        proved("Přesun produktu", () -> {
            Produkt p = sklady.zpristupniProdukt(enumPozice.AKTUALNI, zdroj);   // jen pro zápis do logu
            sklady.presunProdukt(zdroj, cil);
            log("Přesun: " + p + " ze skladu " + zdroj.getCislo() + " na konec skladu " + cil.getCislo());
        });
    }

    private void generuj() {
        proved("Generátor", () -> {
            int pocet = celeCislo(tfPocet.getText(), "Počet");
            if (pocet < 1 || pocet > 10_000) {
                throw new IllegalArgumentException("Počet musí být 1 až 10 000.");
            }
            enumSklad vybrany = vybranySklad();
            boolean nahodne = rbNahodne.isSelected() || vybrany == null;
            for (int i = 0; i < pocet; i++) {
                enumSklad cil = nahodne ? generator.nahodnySklad() : vybrany;
                sklady.vlozProdukt(generator.dalsi(), enumPozice.POSLEDNI, cil);
            }
            log("Generátor: vytvořeno " + pocet + " produktů Apple "
                    + (nahodne ? "(náhodně do všech skladů)." : "do skladu " + vybrany.getCislo() + "."));
        });
    }

    private void prumernaCena() {
        enumSklad s = vybranySklad();
        float prumer = sklady.zjistiPrumernouCenu(s);
        String kde = s == null ? "všech skladů" : "skladu " + s.getCislo();
        log(String.format(Locale.ROOT, "Průměrná cena za kus %s: %.2f Kč", kde, prumer));
        obnov();
    }

    private void zapniFiltr() {
        proved("Filtr pod limitem", () -> {
            int limit = celeCislo(tfLimit.getText(), "Limit");
            aktivniLimit = limit;
            enumSklad s = vybranySklad();
            int nalezeno = sklady.zobrazProduktyPodLimitem(s, limit).length;
            log("Produkty s množstvím menším než " + limit + " (" + (s == null ? "všechny sklady" : s.getNazev())
                    + "): " + nalezeno);
        });
    }

    private void zrus(boolean vse) {
        enumSklad s = vse ? null : vybranySklad();
        if (!vse && s == null) {
            chyba("Zrušení skladu", "Vyberte nahoře konkrétní sklad, nebo použijte \"Zrušit všechny sklady\".");
            return;
        }
        String co = vse ? "všechny sklady" : s.getNazev().toLowerCase(Locale.ROOT);
        Alert dotaz = new Alert(Alert.AlertType.CONFIRMATION,
                "Opravdu zrušit " + co + "? Produkty budou smazány.", ButtonType.YES, ButtonType.NO);
        dotaz.initOwner(okno);
        dotaz.setHeaderText("Zrušení skladu");
        if (dotaz.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        proved("Zrušení skladu", () -> {
            sklady.zrus(s);
            log("Zrušeno: " + co + ".");
        });
    }

    // ---------------------------------------------------------------- soubory

    private void importuj() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import dat (sklad;id;nazev;kategorie;cena;mnozstvi)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV soubory", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("Všechny soubory", "*.*"));
        nastavSlozku(fc);
        File f = fc.showOpenDialog(okno);
        if (f == null) {
            return;
        }
        proved("Import dat", () -> {
            int n = sklady.importData(f.getPath());
            log("Import ze souboru " + f.getName() + ": načteno " + n + " produktů.");
        });
    }

    private void ulozVypis() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Uložit výpis tabulky");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV soubory", "*.csv"));
        fc.setInitialFileName("vypis.csv");
        nastavSlozku(fc);
        File f = fc.showSaveDialog(okno);
        if (f == null) {
            return;
        }
        // stejný formát jako vstupní soubor, takže výpis jde zase importovat
        List<String> vystup = new ArrayList<>();
        vystup.add("sklad;id;nazev;kategorie;cena;mnozstvi");
        for (Radek r : radky) {
            Produkt p = r.produkt();
            vystup.add(String.format(Locale.ROOT, "%d;%s;%s;%s;%.2f;%d", r.sklad().getCislo(),
                    p.getId(), p.getNazev(), p.getKategorie(), p.getCena(), p.getMnozstvi()));
        }
        try {
            Files.write(f.toPath(), vystup, StandardCharsets.UTF_8);
            log("Výpis (" + radky.size() + " produktů) uložen do " + f.getName() + ".");
        } catch (IOException e) {
            chyba("Uložení výpisu", e.getMessage());
        }
    }

    private void ulozLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Uložit log");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Textové soubory", "*.txt"));
        fc.setInitialFileName("log.txt");
        nastavSlozku(fc);
        File f = fc.showSaveDialog(okno);
        if (f == null) {
            return;
        }
        try {
            Files.writeString(f.toPath(), taLog.getText(), StandardCharsets.UTF_8);
            log("Log uložen do " + f.getName() + ".");
        } catch (IOException e) {
            chyba("Uložení logu", e.getMessage());
        }
    }

    private void nastavSlozku(FileChooser fc) {
        File slozka = new File(System.getProperty("user.dir"));
        if (slozka.isDirectory()) {
            fc.setInitialDirectory(slozka);
        }
    }

    // ================================================================ zobrazení

    // znovu naplní tabulku, popisky skladů a stavový řádek podle aktuálního stavu skladů
    private void obnov() {
        enumSklad vybrany = vybranySklad();
        radky.clear();
        for (enumSklad s : enumSklad.values()) {
            if (vybrany != null && s != vybrany) {
                continue;
            }
            Produkt aktualni = aktualniProdukt(s);
            Produkt[] produkty = aktivniLimit == null
                    ? sklady.zobrazProdukty(s)
                    : sklady.zobrazProduktyPodLimitem(s, aktivniLimit);
            for (Produkt p : produkty) {
                radky.add(new Radek(s, p, p == aktualni));
            }
        }

        // počty produktů na přepínačích skladů
        int celkem = 0;
        for (enumSklad s : enumSklad.values()) {
            int n = sklady.zobrazProdukty(s).length;
            celkem += n;
            tlSklady[s.getIndex()].setText(s.getNazev() + "  " + n);
        }
        tlVsechny.setText("Všechny sklady  " + celkem);

        String kde = vybrany == null ? "Všechny sklady" : vybrany.getNazev();
        String filtr = aktivniLimit == null ? "" : "  ·  množství < " + aktivniLimit;
        lblNadpis.setText(kde + "  ·  " + radky.size() + " produktů" + filtr);

        float prumer = sklady.zjistiPrumernouCenu(vybrany);
        String prumerText = String.format(Locale.ROOT, "%.2f Kč", prumer);
        lblPrumerVelky.setText(prumerText + (vybrany == null ? "  (všechny sklady)" : "  (" + vybrany.getNazev() + ")"));
        lblPrumer.setText("Průměrná cena za kus: " + prumerText);
        lblPocet.setText("Produktů celkem: " + celkem);
    }

    // aktuální produkt skladu, nebo null, když je sklad prázdný
    private Produkt aktualniProdukt(enumSklad s) {
        try {
            return sklady.zpristupniProdukt(enumPozice.AKTUALNI, s);   // zpristupniAktualni pozici nemění
        } catch (RuntimeException e) {
            return null;
        }
    }

    // vybere v tabulce řádek s aktuálním prvkem a posune na něj
    private void oznacAktualni(enumSklad s) {
        for (int i = 0; i < radky.size(); i++) {
            Radek r = radky.get(i);
            if (r.aktualni() && r.sklad() == s) {
                tabulka.getSelectionModel().select(i);
                tabulka.scrollTo(i);
                return;
            }
        }
        tabulka.getSelectionModel().clearSelection();
    }

    // ================================================================ pomocné

    // null = "Všechny sklady"
    private enumSklad vybranySklad() {
        Toggle t = skupinaSkladu.getSelectedToggle();
        if (t == null || t == tlVsechny) {
            return null;
        }
        return (enumSklad) t.getUserData();
    }

    private enumPozice vybranaPozice() {
        Toggle t = skupinaPozice.getSelectedToggle();
        return t == null ? enumPozice.POSLEDNI : (enumPozice) t.getUserData();
    }

    private Produkt produktZFormulare() {
        String cenaText = tfCena.getText().trim().replace(',', '.');
        double cena;
        try {
            cena = Double.parseDouble(cenaText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cena musí být číslo, např. 1190,00.");
        }
        int mnozstvi = celeCislo(tfMnozstvi.getText(), "Množství");
        return new Produkt(tfId.getText(), tfNazev.getText(), tfKategorie.getText(), cena, mnozstvi);
    }

    private static int celeCislo(String text, String coToJe) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(coToJe + " musí být celé číslo.");
        }
    }

    // provede operaci; chybu zapíše do logu a ukáže v dialogu. Vrací true, když operace prošla.
    private boolean proved(String nazevOperace, Runnable akce) {
        boolean ok;
        try {
            akce.run();
            ok = true;
        } catch (RuntimeException e) {
            log("CHYBA (" + nazevOperace + "): " + e.getMessage());
            chyba(nazevOperace, e.getMessage());
            ok = false;
        }
        obnov();
        return ok;
    }

    private void chyba(String nadpis, String zprava) {
        Alert a = new Alert(Alert.AlertType.WARNING, zprava, ButtonType.OK);
        a.initOwner(okno);
        a.setHeaderText("Operace se nepodařila: " + nadpis);
        a.showAndWait();
    }

    private void log(String zprava) {
        taLog.appendText(LocalTime.now().format(CAS) + "  " + zprava + "\n");
    }
}
