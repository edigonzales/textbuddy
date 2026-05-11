import { mountAdvisorPdfViewer } from "./editor/advisor-pdf-viewer";
import "./style.css";
import { mountAdvisorValidation } from "./editor/advisor-validation";
import { mountEditorIsland } from "./editor/editor-island";
import { mountInspectorTabs } from "./editor/inspector-tabs";
import { mountTextStatisticsPanel } from "./editor/text-statistics-panel";
import { initializeUiI18n } from "./editor/ui-i18n";

initializeUiI18n();
mountInspectorTabs();
mountEditorIsland();
mountAdvisorValidation();
mountAdvisorPdfViewer();
mountTextStatisticsPanel();
