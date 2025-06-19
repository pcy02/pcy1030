import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class CurrencyConverter extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    // 실시간 환율 저장 (1 단위 외화당 KRW)
    private Map<String, Double> rates = new HashMap<>();

    public CurrencyConverter() {
        super("Currency Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(550, 450);
        setLocationRelativeTo(null);

        // 대상 통화 키 설정
        String[] currencies = { "USD", "JPY", "EUR", "CNY" };
        for (String cur : currencies) {
            rates.put(cur, 0.0);  // 초기값
        }

        // 1) 실시간 환율 가져오기
        fetchRates();

        // 2) 카드 1: 국가 선택 화면 (3행×4열: 국기 / 버튼 / 환율)
        mainPanel.add(buildCountrySelectionPanel(currencies), "SEL");

        // 3) 카드 2~5: 통화 변환 화면 (각 통화별)
        for (String cur : currencies) {
            mainPanel.add(buildConverterPanel(cur), cur);
        }

        add(mainPanel);
        cardLayout.show(mainPanel, "SEL");
        setVisible(true);
    }

    /** 실시간 환율을 exchangerate-api.com API로부터 가져와 rates 맵을 업데이트 */
    private void fetchRates() {
        String apiUrl = "https://api.exchangerate-api.com/v4/latest/KRW";
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONObject jsonRates = json.getJSONObject("rates");

            // API가 반환하는 값은 1 KRW 당 외화 단위 → 외화 당 KRW 단위로 역산
            for (String cur : rates.keySet()) {
                double perKRW = jsonRates.getDouble(cur);
                rates.put(cur, 1.0 / perKRW);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "실시간 환율 가져오기 실패:\n" + e.getMessage(),
                "네트워크 오류", JOptionPane.ERROR_MESSAGE);
            // 실패 시 기본값 유지
        }
    }

    /** 국가 선택 화면 생성: 1행 국기(스케일링), 1행 버튼, 1행 현재 환율 */
    private JPanel buildCountrySelectionPanel(String[] countries) {
        JPanel p = new JPanel(new GridLayout(3, countries.length, 10, 10));
        String[] imgFiles = { "us.png", "jp.png", "eu.png", "cn.png" };

        final int ICON_W = 100;  // 아이콘 너비
        final int ICON_H = 60;   // 아이콘 높이

        // 1) 국기 행 (스케일링)
        for (int i = 0; i < countries.length; i++) {
            ImageIcon orig = new ImageIcon(getClass().getResource(imgFiles[i]));
            Image scaled = orig.getImage()
                               .getScaledInstance(ICON_W, ICON_H, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);

            JLabel lbl = new JLabel(icon, JLabel.CENTER);
            lbl.setPreferredSize(new Dimension(ICON_W, ICON_H));
            p.add(lbl);
        }

        // 2) 버튼 행
        for (String cur : countries) {
            JButton btn = new JButton("→ " + cur + " 변환");
            btn.addActionListener(e -> cardLayout.show(mainPanel, cur));
            p.add(btn);
        }

        // 3) 환율 표시 행
        for (String cur : countries) {
            double rate = rates.get(cur);
            JLabel lblRate = new JLabel(
                String.format("1 %s = %.2f KRW", cur, rate),
                JLabel.CENTER
            );
            p.add(lblRate);
        }

        return p;
    }

    /** 변환 패널 생성: 선택된 통화의 환율과 입력/출력 필드, 뒤로가기 버튼 */
    private JPanel buildConverterPanel(String currency) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 환율 표시
        double rate = rates.get(currency);
        JLabel lblCurrentRate = new JLabel(
            String.format("현재 환율: 1 %s = %.2f KRW", currency, rate)
        );
        lblCurrentRate.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblCurrentRate);
        p.add(Box.createVerticalStrut(15));

        // 1) KRW → 외화
        JLabel lblKRW = new JLabel("원화(KRW) 입력:");
        JTextField tfKRW = new JTextField();
        restrictToDigits(tfKRW);
        JButton btnKRWtoForeign = new JButton("KRW → " + currency);
        JTextField tfToForeign = new JTextField();
        tfToForeign.setEditable(false);
        btnKRWtoForeign.addActionListener(e -> {
            try {
                double krw = Double.parseDouble(tfKRW.getText());
                tfToForeign.setText(String.format("%.2f", krw / rate));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자를 정확히 입력해주세요.",
                    "입력 오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 2) 외화 → KRW
        JLabel lblForeign = new JLabel(currency + " 입력:");
        JTextField tfForeign = new JTextField();
        restrictToDigits(tfForeign);
        JButton btnForeignToKRW = new JButton(currency + " → KRW");
        JTextField tfToKRW = new JTextField();
        tfToKRW.setEditable(false);
        btnForeignToKRW.addActionListener(e -> {
            try {
                double foreign = Double.parseDouble(tfForeign.getText());
                tfToKRW.setText(String.format("%.2f", foreign * rate));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자를 정확히 입력해주세요.",
                    "입력 오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 뒤로가기
        JButton btnBack = new JButton("← 국가 선택으로");
        btnBack.addActionListener(e -> {
            tfKRW.setText("");
            tfToForeign.setText("");
            tfForeign.setText("");
            tfToKRW.setText("");
            cardLayout.show(mainPanel, "SEL");
        });

        // 컴포넌트 배치
        p.add(lblKRW);
        p.add(tfKRW);
        p.add(Box.createVerticalStrut(5));
        p.add(btnKRWtoForeign);
        p.add(Box.createVerticalStrut(5));
        p.add(new JLabel(currency + " 환산액:"));
        p.add(tfToForeign);
        p.add(Box.createVerticalStrut(15));
        p.add(lblForeign);
        p.add(tfForeign);
        p.add(Box.createVerticalStrut(5));
        p.add(btnForeignToKRW);
        p.add(Box.createVerticalStrut(5));
        p.add(new JLabel("원화(KRW) 환산액:"));
        p.add(tfToKRW);
        p.add(Box.createVerticalStrut(20));
        p.add(btnBack);

        return p;
    }

    /** 숫자 및 소수점만 입력 가능하도록 제한 */
    private void restrictToDigits(JTextField tf) {
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final String regex = "[0-9\\.]*";
            @Override
            public void replace(FilterBypass fb, int offs, int len, String text, AttributeSet attrs)
                    throws BadLocationException {
                String newText = fb.getDocument().getText(0, fb.getDocument().getLength());
                newText = newText.substring(0, offs) + text + newText.substring(offs + len);
                if (newText.matches(regex)) {
                    super.replace(fb, offs, len, text, attrs);
                }
            }
            @Override
            public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
                    throws BadLocationException {
                replace(fb, offs, 0, str, a);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CurrencyConverter());
    }
}
