-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 14 mai 2026 à 02:49
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `projet_java`
--

-- --------------------------------------------------------

--
-- Structure de la table `appels`
--

CREATE TABLE `appels` (
  `id_appel` int(11) NOT NULL,
  `id_appelant` int(11) NOT NULL,
  `id_conversation` int(11) NOT NULL,
  `type_appel` enum('audio','video') NOT NULL,
  `date_appel` datetime DEFAULT current_timestamp(),
  `duree_secondes` int(11) DEFAULT 0,
  `statut` enum('manque','accepte','refuse','en_cours') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `appels`
--

INSERT INTO `appels` (`id_appel`, `id_appelant`, `id_conversation`, `type_appel`, `date_appel`, `duree_secondes`, `statut`) VALUES
(1, 3, 2, 'audio', '2026-05-06 13:29:16', 0, 'accepte'),
(2, 3, 2, 'video', '2026-05-06 13:29:29', 11, 'accepte'),
(3, 3, 2, 'video', '2026-05-07 13:45:41', 27, 'accepte');

-- --------------------------------------------------------

--
-- Structure de la table `contacts`
--

CREATE TABLE `contacts` (
  `id_contact` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `id_contact_utilisateur` int(11) NOT NULL,
  `nom_affiche` varchar(100) DEFAULT NULL,
  `est_bloque` tinyint(1) DEFAULT 0,
  `date_ajout` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `contacts`
--

INSERT INTO `contacts` (`id_contact`, `id_utilisateur`, `id_contact_utilisateur`, `nom_affiche`, `est_bloque`, `date_ajout`) VALUES
(22, 2, 3, 'SAM', 0, '2026-05-14 01:44:10'),
(23, 3, 2, 'PENDING', 0, '2026-05-14 01:44:10'),
(24, 21, 2, 'maryam', 0, '2026-05-14 02:09:03'),
(25, 2, 21, 'TEST', 0, '2026-05-14 02:09:03'),
(26, 2, 25, 'TEST0', 0, '2026-05-14 02:41:50'),
(27, 25, 2, 'maryam', 0, '2026-05-14 02:41:50');

-- --------------------------------------------------------

--
-- Structure de la table `conversations`
--

CREATE TABLE `conversations` (
  `id_conversation` int(11) NOT NULL,
  `type_conversation` enum('individuelle','groupe') NOT NULL,
  `nom_groupe` varchar(100) DEFAULT NULL,
  `date_creation` datetime DEFAULT current_timestamp(),
  `date_dernier_message` datetime DEFAULT NULL,
  `id_createur` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `conversations`
--

INSERT INTO `conversations` (`id_conversation`, `type_conversation`, `nom_groupe`, `date_creation`, `date_dernier_message`, `id_createur`) VALUES
(2, 'individuelle', NULL, '2026-04-28 21:18:17', '2026-05-13 21:48:38', NULL),
(3, 'individuelle', NULL, '2026-04-30 01:39:33', '2026-04-30 12:09:58', NULL),
(4, 'individuelle', NULL, '2026-04-30 08:28:01', '2026-04-30 08:28:01', NULL),
(5, 'individuelle', NULL, '2026-05-06 14:47:40', '2026-05-07 13:46:18', NULL),
(6, 'individuelle', NULL, '2026-05-06 15:26:04', '2026-05-06 20:54:27', NULL),
(7, 'individuelle', NULL, '2026-05-06 20:57:18', '2026-05-06 20:57:18', NULL),
(8, 'individuelle', NULL, '2026-05-07 13:48:39', '2026-05-07 13:48:39', NULL),
(9, 'individuelle', NULL, '2026-05-07 13:51:50', '2026-05-07 13:51:50', NULL),
(10, 'individuelle', NULL, '2026-05-13 20:27:39', '2026-05-13 20:30:19', NULL),
(11, 'individuelle', NULL, '2026-05-14 01:45:44', '2026-05-14 02:10:03', NULL),
(12, 'individuelle', NULL, '2026-05-14 02:09:44', '2026-05-14 02:10:08', NULL),
(13, 'individuelle', NULL, '2026-05-14 02:42:06', '2026-05-14 02:42:15', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `groupes`
--

CREATE TABLE `groupes` (
  `id_groupe` int(11) NOT NULL,
  `nom_groupe` varchar(120) NOT NULL,
  `numero_createur` varchar(30) NOT NULL,
  `date_creation` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `groupes`
--

INSERT INTO `groupes` (`id_groupe`, `nom_groupe`, `numero_createur`, `date_creation`) VALUES
(4, 'NEW', '0611221122', '2026-05-14 00:13:02');

-- --------------------------------------------------------

--
-- Structure de la table `groupes_membres`
--

CREATE TABLE `groupes_membres` (
  `id_groupe` int(11) NOT NULL,
  `numero_membre` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `groupes_membres`
--

INSERT INTO `groupes_membres` (`id_groupe`, `numero_membre`) VALUES
(4, '0600112211'),
(4, '0611221122'),
(4, '0611223344');

-- --------------------------------------------------------

--
-- Structure de la table `messages`
--

CREATE TABLE `messages` (
  `id_message` int(11) NOT NULL,
  `id_conversation` int(11) NOT NULL,
  `id_expediteur` int(11) NOT NULL,
  `type_message` enum('texte','image','video','audio','fichier') NOT NULL,
  `contenu_texte` text DEFAULT NULL,
  `url_fichier` varchar(500) DEFAULT NULL,
  `nom_fichier` varchar(255) DEFAULT NULL,
  `taille_fichier` bigint(20) DEFAULT NULL,
  `date_envoi` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages`
--

INSERT INTO `messages` (`id_message`, `id_conversation`, `id_expediteur`, `type_message`, `contenu_texte`, `url_fichier`, `nom_fichier`, `taille_fichier`, `date_envoi`) VALUES
(3, 2, 3, 'texte', 'salut', NULL, NULL, NULL, '2026-04-28 21:18:17'),
(4, 2, 2, 'texte', 'salut 2', NULL, NULL, NULL, '2026-04-28 21:20:30'),
(12, 2, 3, 'texte', 'salut', NULL, NULL, NULL, '2026-04-28 21:18:17'),
(13, 2, 2, 'texte', 'salut 2', NULL, NULL, NULL, '2026-04-28 21:20:30'),
(14, 2, 3, 'texte', 'tu as reçu mon message ?', NULL, NULL, NULL, '2026-04-28 21:21:00'),
(15, 2, 2, 'texte', 'oui je l\'ai reçu !', NULL, NULL, NULL, '2026-04-28 21:21:30'),
(16, 2, 3, 'texte', 'le serveur fonctionne bien alors', NULL, NULL, NULL, '2026-04-28 21:22:00'),
(17, 2, 2, 'texte', 'oui tout marche correctement', NULL, NULL, NULL, '2026-04-28 21:22:45'),
(18, 3, 1, 'texte', 'Salut maryam !', NULL, NULL, NULL, '2026-04-29 10:00:00'),
(19, 3, 2, 'texte', 'Salut Jean !', NULL, NULL, NULL, '2026-04-29 10:00:30'),
(20, 3, 1, 'texte', 'Tu as testé la connexion ?', NULL, NULL, NULL, '2026-04-29 10:01:00'),
(21, 3, 2, 'texte', 'Oui ça marche bien', NULL, NULL, NULL, '2026-04-29 10:01:45'),
(22, 3, 1, 'texte', 'Et les messages en attente ?', NULL, NULL, NULL, '2026-04-29 10:02:10'),
(23, 3, 2, 'texte', 'Aussi, j\'ai reçu mes messages offline', NULL, NULL, NULL, '2026-04-29 10:02:55'),
(24, 3, 1, 'texte', 'Parfait le projet avance bien !', NULL, NULL, NULL, '2026-04-29 10:03:30'),
(32, 2, 3, 'texte', 'salut', NULL, NULL, NULL, '2026-04-28 21:18:17'),
(33, 2, 2, 'texte', 'salut 2', NULL, NULL, NULL, '2026-04-28 21:20:30'),
(34, 2, 3, 'texte', 'tu as reçu mon message ?', NULL, NULL, NULL, '2026-04-28 21:21:00'),
(35, 2, 2, 'texte', 'oui je l\'ai reçu !', NULL, NULL, NULL, '2026-04-28 21:21:30'),
(36, 2, 3, 'texte', 'le serveur fonctionne bien alors', NULL, NULL, NULL, '2026-04-28 21:22:00'),
(37, 2, 2, 'texte', 'oui tout marche correctement', NULL, NULL, NULL, '2026-04-28 21:22:45'),
(38, 3, 1, 'texte', 'Salut maryam !', NULL, NULL, NULL, '2026-04-29 10:00:00'),
(39, 3, 2, 'texte', 'Salut Jean !', NULL, NULL, NULL, '2026-04-29 10:00:30'),
(40, 3, 1, 'texte', 'Tu as testé la connexion ?', NULL, NULL, NULL, '2026-04-29 10:01:00'),
(41, 3, 2, 'texte', 'Oui ça marche bien', NULL, NULL, NULL, '2026-04-29 10:01:45'),
(42, 3, 1, 'texte', 'Et les messages en attente ?', NULL, NULL, NULL, '2026-04-29 10:02:10'),
(43, 3, 2, 'texte', 'Aussi, j\'ai reçu mes messages offline', NULL, NULL, NULL, '2026-04-29 10:02:55'),
(44, 3, 1, 'texte', 'Parfait le projet avance bien !', NULL, NULL, NULL, '2026-04-29 10:03:30'),
(45, 2, 3, 'texte', 'SALM', NULL, NULL, NULL, '2026-04-30 12:07:23'),
(46, 3, 2, 'texte', 'LABAS', NULL, NULL, NULL, '2026-04-30 12:07:28'),
(47, 2, 3, 'texte', 'HI', NULL, NULL, NULL, '2026-04-30 12:07:42'),
(48, 3, 2, 'texte', 'labs', NULL, NULL, NULL, '2026-04-30 12:07:56'),
(49, 3, 2, 'texte', 'hh', NULL, NULL, NULL, '2026-04-30 12:09:26'),
(50, 3, 2, 'texte', 'daba daba', NULL, NULL, NULL, '2026-04-30 12:09:58'),
(51, 2, 2, 'texte', 'SL', NULL, NULL, NULL, '2026-04-30 12:12:17'),
(52, 2, 3, 'texte', 'hi', NULL, NULL, NULL, '2026-04-30 12:12:36'),
(53, 2, 2, 'texte', 'cc', NULL, NULL, NULL, '2026-05-06 12:13:02'),
(54, 2, 2, 'texte', 'SALU', NULL, NULL, NULL, '2026-05-06 13:28:54'),
(55, 2, 2, 'texte', 'SALM', NULL, NULL, NULL, '2026-05-06 13:46:58'),
(56, 2, 2, 'texte', 'SLM', NULL, NULL, NULL, '2026-05-06 14:24:01'),
(57, 5, 10, 'texte', 'HI', NULL, NULL, NULL, '2026-05-06 14:47:40'),
(58, 5, 10, 'texte', 'HI', NULL, NULL, NULL, '2026-05-06 14:49:01'),
(59, 5, 2, 'texte', 'SALI', NULL, NULL, NULL, '2026-05-06 14:50:13'),
(60, 6, 11, 'texte', 'salut1', NULL, NULL, NULL, '2026-05-06 15:26:04'),
(61, 6, 2, 'texte', 'salam', NULL, NULL, NULL, '2026-05-06 20:54:27'),
(62, 7, 2, 'texte', 'bjr', NULL, NULL, NULL, '2026-05-06 20:57:18'),
(63, 5, 2, 'texte', 'salut', NULL, NULL, NULL, '2026-05-07 13:46:18'),
(64, 2, 2, 'texte', 'hi', NULL, NULL, NULL, '2026-05-07 13:46:26'),
(65, 2, 3, 'texte', 'hana', NULL, NULL, NULL, '2026-05-07 13:46:31'),
(66, 8, 3, 'texte', 'salm', NULL, NULL, NULL, '2026-05-07 13:48:39'),
(67, 9, 11, 'texte', 'hi', NULL, NULL, NULL, '2026-05-07 13:51:50'),
(68, 2, 2, 'texte', 'ha', NULL, NULL, NULL, '2026-05-13 20:21:57'),
(69, 10, 13, 'texte', 'salm', NULL, NULL, NULL, '2026-05-13 20:27:39'),
(70, 10, 2, 'texte', 'hi', NULL, NULL, NULL, '2026-05-13 20:29:15'),
(71, 10, 2, 'texte', 'hi', NULL, NULL, NULL, '2026-05-13 20:30:19'),
(72, 2, 2, 'texte', 'salm', NULL, NULL, NULL, '2026-05-13 21:48:37'),
(73, 11, 3, 'texte', 'SALM', NULL, NULL, NULL, '2026-05-14 01:45:44'),
(74, 12, 21, 'texte', 'HI', NULL, NULL, NULL, '2026-05-14 02:09:44'),
(75, 11, 2, 'texte', 'SALM', NULL, NULL, NULL, '2026-05-14 02:10:03'),
(76, 12, 21, 'texte', 'HI', NULL, NULL, NULL, '2026-05-14 02:10:08'),
(77, 13, 2, 'texte', 'HI', NULL, NULL, NULL, '2026-05-14 02:42:06'),
(78, 13, 25, 'texte', 'SALM', NULL, NULL, NULL, '2026-05-14 02:42:15');

-- --------------------------------------------------------

--
-- Structure de la table `messages_file_attente`
--

CREATE TABLE `messages_file_attente` (
  `id_file` int(11) NOT NULL,
  `id_message` int(11) NOT NULL,
  `id_destinataire` int(11) NOT NULL,
  `est_delivre` tinyint(1) DEFAULT 0,
  `date_mise_file` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages_file_attente`
--

INSERT INTO `messages_file_attente` (`id_file`, `id_message`, `id_destinataire`, `est_delivre`, `date_mise_file`) VALUES
(1, 4, 3, 1, '2026-04-28 21:20:30'),
(2, 24, 2, 1, '2026-04-30 01:39:33'),
(4, 46, 1, 0, '2026-04-30 12:07:28'),
(5, 48, 1, 0, '2026-04-30 12:07:56'),
(6, 49, 1, 0, '2026-04-30 12:09:26'),
(7, 50, 1, 0, '2026-04-30 12:09:58'),
(8, 53, 3, 1, '2026-05-06 12:13:02'),
(9, 55, 3, 1, '2026-05-06 13:46:58'),
(10, 56, 3, 1, '2026-05-06 14:24:01'),
(11, 58, 2, 1, '2026-05-06 14:49:01'),
(15, 68, 3, 1, '2026-05-13 20:21:57'),
(16, 69, 2, 1, '2026-05-13 20:27:39'),
(17, 72, 3, 1, '2026-05-13 21:48:38'),
(18, 75, 3, 0, '2026-05-14 02:10:03');

-- --------------------------------------------------------

--
-- Structure de la table `messages_groupes`
--

CREATE TABLE `messages_groupes` (
  `id_message` int(11) NOT NULL,
  `id_groupe` int(11) NOT NULL,
  `telephone_expediteur` varchar(30) NOT NULL,
  `nom_expediteur` varchar(120) NOT NULL,
  `contenu` text DEFAULT NULL,
  `date_envoi` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages_groupes`
--

INSERT INTO `messages_groupes` (`id_message`, `id_groupe`, `telephone_expediteur`, `nom_expediteur`, `contenu`, `date_envoi`) VALUES
(10, 4, '0611221122', 'maryam', 'SALMO', '2026-05-14 00:13:16'),
(11, 4, '0611223344', 'TEST', 'HI', '2026-05-14 00:13:28');

-- --------------------------------------------------------

--
-- Structure de la table `messages_lus`
--

CREATE TABLE `messages_lus` (
  `id_message` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `date_lecture` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `participants_conversation`
--

CREATE TABLE `participants_conversation` (
  `id_participant` int(11) NOT NULL,
  `id_conversation` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `date_join` datetime DEFAULT current_timestamp(),
  `est_admin` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `participants_conversation`
--

INSERT INTO `participants_conversation` (`id_participant`, `id_conversation`, `id_utilisateur`, `date_join`, `est_admin`) VALUES
(22, 11, 3, '2026-05-14 01:45:44', 0),
(23, 11, 2, '2026-05-14 01:45:44', 0),
(24, 12, 21, '2026-05-14 02:09:44', 0),
(25, 12, 2, '2026-05-14 02:09:44', 0),
(26, 13, 2, '2026-05-14 02:42:06', 0),
(27, 13, 25, '2026-05-14 02:42:06', 0);

-- --------------------------------------------------------

--
-- Structure de la table `utilisateurs`
--

CREATE TABLE `utilisateurs` (
  `id_utilisateur` int(11) NOT NULL,
  `nom_complet` varchar(100) NOT NULL,
  `numero_telephone` varchar(20) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `date_inscription` datetime DEFAULT current_timestamp(),
  `derniere_connexion` datetime DEFAULT NULL,
  `photo_profil` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `utilisateurs`
--

INSERT INTO `utilisateurs` (`id_utilisateur`, `nom_complet`, `numero_telephone`, `mot_de_passe`, `date_inscription`, `derniere_connexion`, `photo_profil`) VALUES
(1, 'Jean', '0612345678', '$2a$10$/F6CyQyTNCPfRbqnhNeK2.Gs3I5HAFSAohRkrEHd94p2rtw294iWS', '2026-04-25 19:05:19', '2026-04-25 19:17:27', NULL),
(2, 'maryam', '0611221122', '$2a$10$vHYnCEgdZP5SihT91Rsv7elKWimoX7mW.F8fBvuSQC41DR/PY6lti', '2026-04-28 20:19:41', '2026-05-14 02:37:43', NULL),
(3, 'samira', '0600112211', '$2a$10$oegmo5bSGDOs8WM.D0XnKu5sjkZbRFNnP2xHj4fe115rrNwZ0exBC', '2026-04-28 20:45:13', '2026-05-14 02:06:13', NULL),
(4, 'hanane', '0691395520', '$2a$10$Ig3ILD2d1Th/4iD0gLE6g.f3XI1P3ktm69H886AsNvId1vm.HPY1i', '2026-04-30 12:14:58', '2026-04-30 12:16:16', NULL),
(10, 'TESTcONT', '0600001111', '$2a$10$kgUSJmudDW40CZDA9pCAzu9KJwMf4alVtJN2sCS.03hOwHA8RLjqW', '2026-05-06 14:47:02', '2026-05-06 14:47:15', NULL),
(11, 'testC', '0600110011', '$2a$10$M3pmBVHbj3ie7amPS2RJp.VUKB9cLXXW9EcYjFEvfsUj2jrFdPMIq', '2026-05-06 15:24:32', '2026-05-07 15:44:55', NULL),
(12, 'rr', '0687654321', '$2a$10$q/piaZIEY/V9uDGQ.goAM.CABclozCf7cTWY4Dh2klKer1ZakQm2G', '2026-05-06 20:56:20', '2026-05-06 20:56:37', NULL),
(13, 'nom', '0699009900', '$2a$10$6QguM1oguM9MY2ML1D1MNeDG4q0TXw75KH.gL6V2R3ECVByokSY9.', '2026-05-13 20:26:57', '2026-05-13 20:27:06', NULL),
(14, 'salima', '0600000009', '$2a$10$E.5RVPjbTs7rxBfpy7lUk.q6ksOi/kzoMdKh8uLGdbDzIXKA82bT6', '2026-05-13 23:54:11', NULL, NULL),
(15, 'karima', '0600000006', '$2a$10$QzTbx3r6.9XxjmDSuAIDzOpYE0NIKRX7N8jEA5C5CTllQwawxpE2.', '2026-05-13 23:57:45', '2026-05-14 00:38:21', NULL),
(16, 'mohamed', '0600000005', '$2a$10$68ovhj..K6mukcCfhLSIH.eXIYDF46axWxtDw6mQz3bdoUCcqwBgS', '2026-05-14 00:37:59', NULL, NULL),
(17, 'test2', '0611111111', '$2a$10$/BuY2vmPJmr7omvvyLfTiuDKQWXxUiueRs4LygLDx.cQZwQjNXYNK', '2026-05-14 01:07:28', NULL, NULL),
(18, 'test3', '0633333333', '$2a$10$/3RD6vJXCJNLlA5Cc9OUseVH1f8DQ1R7S/4vducgWslUJKGReGH72', '2026-05-14 01:45:06', NULL, NULL),
(19, 'test4', '0644444444', '$2a$10$Z1QRCJ5lRtdwFBUGR4w/5OtZk4wbAXvpbcHfPPJe4Ny/AabNnnVv6', '2026-05-14 01:56:34', '2026-05-14 01:57:08', NULL),
(20, 'test5', '0655555555', '$2a$10$McrAhEOd3bo6aqd4KUZbV.mnXQyoua6gy.3mf9LIR6r6h1j4pzv4.', '2026-05-14 02:02:09', NULL, NULL),
(21, 'TEST', '0611223344', '$2a$10$ixZgeUeDYUByps393DlMcOZ6vEWUBRA8H5v2v62TbDhAw.zOdKgYK', '2026-05-14 02:08:29', '2026-05-14 02:08:44', NULL),
(22, 'test6', '0666666666', '$2a$10$dNHe4OsxPHIzT66eHmpM.O1TPQIj856I/eH0UQJzUJLDE8hR5XM4S', '2026-05-14 02:10:34', NULL, NULL),
(23, 'test7', '0677777777', '$2a$10$Dsqqbk4iFEuewRLCgY/UXezRBbao5sjiwwsTzZLRbMXyWABavYdHW', '2026-05-14 02:27:04', '2026-05-14 02:28:03', NULL),
(24, 'test8', '0688888888', '$2a$10$S/S.4Iz8ViHAazACjWBMTe01HQy6Iu/hDJX8rxogDg1VclEx4ssTm', '2026-05-14 02:39:47', NULL, NULL),
(25, 'TEST0', '0600990099', '$2a$10$tfuvCHn86y8rP6tx1QAgOOb3Ay0LhfxCcaFczcbdQWX0dEsWYW5fG', '2026-05-14 02:40:55', '2026-05-14 02:41:09', NULL);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `appels`
--
ALTER TABLE `appels`
  ADD PRIMARY KEY (`id_appel`),
  ADD KEY `idx_appel_appelant` (`id_appelant`),
  ADD KEY `idx_appel_conv` (`id_conversation`);

--
-- Index pour la table `contacts`
--
ALTER TABLE `contacts`
  ADD PRIMARY KEY (`id_contact`),
  ADD UNIQUE KEY `uq_contact` (`id_utilisateur`,`id_contact_utilisateur`),
  ADD KEY `fk_contacts_contact` (`id_contact_utilisateur`);

--
-- Index pour la table `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`id_conversation`),
  ADD KEY `fk_conv_createur` (`id_createur`);

--
-- Index pour la table `groupes`
--
ALTER TABLE `groupes`
  ADD PRIMARY KEY (`id_groupe`);

--
-- Index pour la table `groupes_membres`
--
ALTER TABLE `groupes_membres`
  ADD PRIMARY KEY (`id_groupe`,`numero_membre`);

--
-- Index pour la table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id_message`),
  ADD KEY `idx_msg_conv` (`id_conversation`),
  ADD KEY `idx_msg_exp` (`id_expediteur`);

--
-- Index pour la table `messages_file_attente`
--
ALTER TABLE `messages_file_attente`
  ADD PRIMARY KEY (`id_file`),
  ADD UNIQUE KEY `uq_file` (`id_message`,`id_destinataire`),
  ADD KEY `idx_file_dest` (`id_destinataire`);

--
-- Index pour la table `messages_groupes`
--
ALTER TABLE `messages_groupes`
  ADD PRIMARY KEY (`id_message`),
  ADD KEY `id_groupe` (`id_groupe`);

--
-- Index pour la table `messages_lus`
--
ALTER TABLE `messages_lus`
  ADD PRIMARY KEY (`id_message`,`id_utilisateur`),
  ADD KEY `fk_lu_user` (`id_utilisateur`);

--
-- Index pour la table `participants_conversation`
--
ALTER TABLE `participants_conversation`
  ADD PRIMARY KEY (`id_participant`),
  ADD UNIQUE KEY `uq_participant` (`id_conversation`,`id_utilisateur`),
  ADD KEY `fk_part_user` (`id_utilisateur`);

--
-- Index pour la table `utilisateurs`
--
ALTER TABLE `utilisateurs`
  ADD PRIMARY KEY (`id_utilisateur`),
  ADD UNIQUE KEY `uq_telephone` (`numero_telephone`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `appels`
--
ALTER TABLE `appels`
  MODIFY `id_appel` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `contacts`
--
ALTER TABLE `contacts`
  MODIFY `id_contact` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT pour la table `conversations`
--
ALTER TABLE `conversations`
  MODIFY `id_conversation` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `groupes`
--
ALTER TABLE `groupes`
  MODIFY `id_groupe` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `messages`
--
ALTER TABLE `messages`
  MODIFY `id_message` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=79;

--
-- AUTO_INCREMENT pour la table `messages_file_attente`
--
ALTER TABLE `messages_file_attente`
  MODIFY `id_file` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `messages_groupes`
--
ALTER TABLE `messages_groupes`
  MODIFY `id_message` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `participants_conversation`
--
ALTER TABLE `participants_conversation`
  MODIFY `id_participant` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT pour la table `utilisateurs`
--
ALTER TABLE `utilisateurs`
  MODIFY `id_utilisateur` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=26;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `appels`
--
ALTER TABLE `appels`
  ADD CONSTRAINT `fk_appel_appelant` FOREIGN KEY (`id_appelant`) REFERENCES `utilisateurs` (`id_utilisateur`),
  ADD CONSTRAINT `fk_appel_conv` FOREIGN KEY (`id_conversation`) REFERENCES `conversations` (`id_conversation`);

--
-- Contraintes pour la table `contacts`
--
ALTER TABLE `contacts`
  ADD CONSTRAINT `fk_contacts_contact` FOREIGN KEY (`id_contact_utilisateur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_contacts_user` FOREIGN KEY (`id_utilisateur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE;

--
-- Contraintes pour la table `conversations`
--
ALTER TABLE `conversations`
  ADD CONSTRAINT `fk_conv_createur` FOREIGN KEY (`id_createur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE SET NULL;

--
-- Contraintes pour la table `groupes_membres`
--
ALTER TABLE `groupes_membres`
  ADD CONSTRAINT `groupes_membres_ibfk_1` FOREIGN KEY (`id_groupe`) REFERENCES `groupes` (`id_groupe`) ON DELETE CASCADE;

--
-- Contraintes pour la table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `fk_msg_conv` FOREIGN KEY (`id_conversation`) REFERENCES `conversations` (`id_conversation`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_msg_exp` FOREIGN KEY (`id_expediteur`) REFERENCES `utilisateurs` (`id_utilisateur`);

--
-- Contraintes pour la table `messages_file_attente`
--
ALTER TABLE `messages_file_attente`
  ADD CONSTRAINT `fk_file_msg` FOREIGN KEY (`id_message`) REFERENCES `messages` (`id_message`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_file_user` FOREIGN KEY (`id_destinataire`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE;

--
-- Contraintes pour la table `messages_groupes`
--
ALTER TABLE `messages_groupes`
  ADD CONSTRAINT `messages_groupes_ibfk_1` FOREIGN KEY (`id_groupe`) REFERENCES `groupes` (`id_groupe`) ON DELETE CASCADE;

--
-- Contraintes pour la table `messages_lus`
--
ALTER TABLE `messages_lus`
  ADD CONSTRAINT `fk_lu_msg` FOREIGN KEY (`id_message`) REFERENCES `messages` (`id_message`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lu_user` FOREIGN KEY (`id_utilisateur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE;

--
-- Contraintes pour la table `participants_conversation`
--
ALTER TABLE `participants_conversation`
  ADD CONSTRAINT `fk_part_conv` FOREIGN KEY (`id_conversation`) REFERENCES `conversations` (`id_conversation`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_part_user` FOREIGN KEY (`id_utilisateur`) REFERENCES `utilisateurs` (`id_utilisateur`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
