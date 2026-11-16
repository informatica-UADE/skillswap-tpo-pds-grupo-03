package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import java.util.List;

/** Inbound read operations for the current user's publications. */
public interface PublicationQueryUseCases {
    List<PublicationView> listMyPublications();
    List<PublicationType> allowedPublicationTypes();
}
