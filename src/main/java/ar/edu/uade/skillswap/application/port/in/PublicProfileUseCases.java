package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileQuery;
import ar.edu.uade.skillswap.application.model.PublicProfileData.PublicProfileView;

/** Consulta del perfil público de cualquier usuario activo (§4.1); no modifica información. */
public interface PublicProfileUseCases {
    PublicProfileView viewPublicProfile(PublicProfileQuery query);
}
