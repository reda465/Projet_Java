package model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageFileAttente {
    private int idFile;
    private int idMessage;
    private int idDestinataire;
    private boolean estDelivre;
    private java.time.LocalDateTime dateMiseFile;
}
