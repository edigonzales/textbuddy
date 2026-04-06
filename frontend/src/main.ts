import { mountAdvisorPdfViewer } from "./editor/advisor-pdf-viewer";
import "./style.css";
import { mountAdvisorValidation } from "./editor/advisor-validation";
import { mountEditorIsland } from "./editor/editor-island";
import { mountTextStatisticsPanel } from "./editor/text-statistics-panel";

mountEditorIsland();
mountAdvisorValidation();
mountAdvisorPdfViewer();
mountTextStatisticsPanel();
