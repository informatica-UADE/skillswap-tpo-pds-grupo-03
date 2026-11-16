package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;

/** Inbound commands that mutate publication aggregates. */
public interface PublicationCommandUseCases {
    PublicationView createDraft(CreateDraftCommand command);
    PublicationView editDraft(EditDraftCommand command);
    PublicationView changeStatus(ChangeStatusCommand command);
}
