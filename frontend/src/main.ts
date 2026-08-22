import "./style.css";
import { mountEditorToolbar } from "./editor/editor-toolbar";
import { mountEditorIsland } from "./editor/editor-island";
import { mountTextStatisticsPanel } from "./editor/text-statistics-panel";
import { initializeUiI18n } from "./editor/ui-i18n";
import { mountWorkspaceShell } from "./editor/workspace-shell";

initializeUiI18n();
mountWorkspaceShell();
mountEditorIsland();
mountEditorToolbar();
mountTextStatisticsPanel();
